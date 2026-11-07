# Mini tuyển dụng

REST API quản lý công ty, tin tuyển dụng và hồ sơ ứng tuyển bằng **Java 17, Spring Boot 3.5.7, PostgreSQL 17, Maven và Flyway**.

## 1. Chuẩn bị

- JDK 17, biến `JAVA_HOME` trỏ tới JDK.
- Docker Desktop đang chạy (hoặc PostgreSQL cài trực tiếp).
- Không cần cài Maven riêng: project có `mvnw` / `mvnw.cmd`.

## 2. Chạy trên Windows / PowerShell

Từ thư mục chứa `pom.xml`, khởi động database:

```powershell
docker compose up -d --wait
```

Database mặc định: `recruitment`, cổng **5433** trên máy, username/password: `recruitment` / `recruitment` (dùng thực hành local). Container vẫn dùng cổng 5432 nội bộ.

Cổng 5433 giúp tránh xung đột với PostgreSQL cài sẵn hoặc container cũ đang dùng 5432. Nếu dùng database khác, đặt `DB_URL` tương ứng.

Chạy backend:

```powershell
.\mvnw.cmd spring-boot:run
```

API: `http://localhost:8080`. Truy cập `GET /companies` hoặc `GET /jobs` để kiểm tra; không có trang giao diện ở `/`.

Nếu cần đổi cấu hình, đặt biến môi trường trước khi chạy:

```powershell
$env:DB_URL = "jdbc:postgresql://localhost:5433/recruitment"
$env:DB_USERNAME = "recruitment"
$env:DB_PASSWORD = "recruitment"
$env:SERVER_PORT = "8080"
.\mvnw.cmd spring-boot:run
```

Compose có thể đọc `DB_PASSWORD` từ `.env` (tham khảo `.env.example`). Java không tự đọc `.env`; nếu đổi mật khẩu hãy đặt cùng biến môi trường cho Java. Mật khẩu khởi tạo chỉ áp dụng khi volume PostgreSQL chưa có dữ liệu.

Linux/macOS: dùng `./mvnw` thay `.\mvnw.cmd` (chạy `chmod +x mvnw` nếu cần).

Build JAR và chạy:

```powershell
.\mvnw.cmd clean verify
java -jar target/recruitment-0.0.1-SNAPSHOT.jar
```

## 3. Migration và mô hình dữ liệu

Flyway tự chạy `src/main/resources/db/migration/V1__create_tables.sql` khi khởi động. Hibernate dùng `ddl-auto: validate`, không tự sửa bảng.

```text
companies (1) ─── (n) jobs (1) ─── (n) applications
```

- Lương dùng `BigDecimal` / `NUMERIC(15,2)`, quy ước VND/tháng; không âm, `salaryFrom <= salaryTo`, tối đa hai chữ số thập phân.
- Trạng thái chỉ nhận `OPEN` / `CLOSED`; client phải cung cấp khi tạo/sửa Job.
- Các trường trong request mẫu đều bắt buộc. Website và CV là URL HTTP/HTTPS.
- Thời gian lưu theo `Instant`, response dùng ISO 8601 / UTC.
- Email được bỏ khoảng trắng đầu/cuối và chuyển chữ thường trước khi validate/lưu. Unique theo **cặp `(job_id, email)`**, không unique toàn hệ thống.
- Nộp vào Job CLOSED: `409`. Email trùng trong cùng Job: `409`. Cùng email ở Job khác: được phép.
- Có kiểm tra ở service và unique constraint ở database. Submit, sửa và xóa Job cùng khóa bản ghi Job trong transaction để xử lý request đồng thời.
- Xóa Company còn Job hoặc Job còn Application: `409`; không cascade xóa hồ sơ.
- `PUT` thay toàn bộ trường có thể chỉnh sửa; `createdAt`, `appliedAt` do server gán.

## 4. API

| Method | Endpoint | Thành công |
|---|---|---|
| POST | `/companies` | 201 + Location |
| GET | `/companies` | 200, danh sách |
| GET | `/companies/{id}` | 200 |
| PUT | `/companies/{id}` | 200 |
| DELETE | `/companies/{id}` | 204 |
| POST | `/jobs` | 201 + Location |
| GET | `/jobs` | 200, phân trang |
| GET | `/jobs/{id}` | 200 |
| PUT | `/jobs/{id}` | 200 |
| DELETE | `/jobs/{id}` | 204 |
| POST | `/jobs/{id}/applications` | 201 |

### Tạo Company

`POST http://localhost:8080/companies`, header `Content-Type: application/json`:

```json
{
  "name": "ACME",
  "address": "Thành phố Hồ Chí Minh",
  "website": "https://example.com"
}
```

Lấy `id` trong response dùng làm `companyId` bên dưới.

### Tạo Job

`POST http://localhost:8080/jobs`:

```json
{
  "companyId": 1,
  "title": "Java Backend Developer",
  "description": "Phát triển REST API bằng Spring Boot",
  "salaryFrom": 15000000,
  "salaryTo": 25000000,
  "location": "HCM",
  "status": "OPEN"
}
```

Sửa bằng `PUT /jobs/{id}` với body đủ các trường trên. Đổi `status` sang `CLOSED` để đóng tin.

### Tìm kiếm Job

```http
GET /jobs?keyword=java&location=hcm&status=OPEN&page=0&size=10&sort=salaryFrom,asc
```

- `keyword`: tìm chuỗi con trong tiêu đề **hoặc** mô tả, không phân biệt hoa thường.
- `location`: tìm chuỗi con không phân biệt hoa thường.
- `status`: so khớp chính xác. Các nhóm điều kiện kết hợp bằng **AND**.
- Bỏ tham số hoặc chuỗi tìm kiếm rỗng thì không lọc theo điều kiện đó; keyword/location tối đa 200 ký tự. `%` và `_` được tìm như ký tự thường.
- `page >= 0`, `1 <= size <= 100`; mặc định `page=0`, `size=10`.
- `sort=field,asc` hoặc `sort=field,desc`, một trường mỗi request. Trường hỗ trợ: `id`, `title`, `salaryFrom`, `salaryTo`, `location`, `status`, `createdAt`.
- Mặc định `createdAt,desc`; thêm `id,asc` làm thứ tự phụ nếu sort theo trường khác.
- Sai phân trang, trường sort, hướng sort hoặc enum: `400`.

Response:

```json
{
  "content": [],
  "page": 0,
  "size": 10,
  "totalElements": 0,
  "totalPages": 0
}
```

### Nộp hồ sơ

`POST http://localhost:8080/jobs/1/applications` (thay `1` bằng ID Job thực tế):

```json
{
  "fullName": "Nguyễn Văn An",
  "email": "an@example.com",
  "cvUrl": "https://example.com/cv/an.pdf"
}
```

Response chứa `id`, `jobId`, `fullName`, `email`, `cvUrl`, `appliedAt`. Không cần upload file, client cung cấp URL CV.

### JSON lỗi thống nhất

```json
{
  "timestamp": "2026-11-07T03:00:00Z",
  "status": 409,
  "code": "DUPLICATE_APPLICATION",
  "message": "Email đã nộp hồ sơ vào Job này",
  "path": "/jobs/1/applications",
  "fieldErrors": {}
}
```

- `400`: thiếu trường, sai định dạng email/URL, khoảng lương sai, JSON lỗi, enum/tham số sai. Validation trả chi tiết trong `fieldErrors`.
- `404`: không có Company/Job hoặc endpoint không tồn tại.
- `409`: Job đóng, hồ sơ trùng, xóa dữ liệu còn tham chiếu hoặc xung đột ràng buộc DB.

## 5. Test

```powershell
.\mvnw.cmd test
```

Mặc định test dùng **H2 ở PostgreSQL compatibility mode**, chạy migration thật, không cần bật Docker. H2 không thay thế hoàn toàn việc kiểm tra PostgreSQL.

- 13 unit test service bằng JUnit 5 / Mockito: tạo dữ liệu, Company không tồn tại, lương sai, OPEN/CLOSED, trùng email trong Job, dùng lại email ở Job khác, chuẩn hóa email, xóa dữ liệu có liên quan, tham số tìm kiếm sai.
- 9 integration test qua MockMvc: CRUD, filter + sort + phân trang, ký tự wildcard, nghiệp vụ nộp hồ sơ, constraint unique, hai request đồng thời, validation và cấu trúc lỗi.
- 1 test khởi động application context.

Có thể chạy cùng bộ test với **database PostgreSQL riêng dành cho test**:

```powershell
# Tạo recruitment_test trước bằng psql hoặc pgAdmin.
$env:TEST_DB_URL = "jdbc:postgresql://localhost:5433/recruitment_test"
$env:TEST_DB_USERNAME = "recruitment"
$env:TEST_DB_PASSWORD = "recruitment"
.\mvnw.cmd test
Remove-Item Env:TEST_DB_URL, Env:TEST_DB_USERNAME, Env:TEST_DB_PASSWORD
```

Integration test xóa dữ liệu ba bảng trước mỗi test; chỉ trỏ `TEST_DB_URL` tới database test.

## 6. Postman

Import `postman/recruitment.postman_collection.json`. Chỉnh `baseUrl` nếu đổi port. Chạy collection theo thứ tự; collection tự lưu Company/Job ID, kiểm tra status và thử cả ca từ chối (trùng email, CLOSED, dữ liệu sai, xóa bản ghi có liên quan).

Cũng có thể chạy bằng Postman CLI khi backend đang chạy:

```powershell
postman collection run postman/recruitment.postman_collection.json --no-report-events
```

Đã kiểm tra build và 23 test trên cả H2 và PostgreSQL 17; collection chạy qua HTTP thật đạt 16/16 request, 22/22 assertion.

## 7. Cấu trúc

```text
src/main/java/com/example/demo/
  company/       Entity, DTO, repository, service, controller
  job/           CRUD, enum trạng thái, Specification tìm kiếm
  application/   Nộp hồ sơ
  common/        Exception handler, JSON lỗi, response phân trang
src/main/resources/db/migration/
src/test/java/com/example/demo/
```

Controller nhận DTO và validate → service xử lý nghiệp vụ/transaction → repository thao tác DB. API trả DTO, không serialize trực tiếp entity.

## 8. Nộp GitHub

Đẩy project vào repository **public**, bao gồm Maven Wrapper, migration, tests, README và Postman collection; không đưa `target`, `.cache`, `.env` vào Git.

Repository local có các commit tách theo cấu hình, mô hình dữ liệu, API, test và tài liệu. Sau khi tạo repo GitHub public trống, thêm remote và đẩy nhánh `main`:

```powershell
git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPOSITORY.git
git push -u origin main
```

Thay URL mẫu bằng repo của bạn. GitHub repository và remote không được tự tạo trong lần triển khai local này.
