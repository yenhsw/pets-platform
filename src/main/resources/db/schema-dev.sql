-- Dev schema: hibernate ddl-auto=update tự tạo bảng từ entity
-- File này chỉ chạy thêm nếu cần index hoặc constraint bổ sung

-- Index cho test_apis (Hibernate không tạo index cho @Column, chỉ tạo cho @Table.indexes)
CREATE INDEX IF NOT EXISTS idx_test_apis_email ON test_apis(email);
CREATE INDEX IF NOT EXISTS idx_test_apis_name ON test_apis(name);
CREATE INDEX IF NOT EXISTS idx_test_apis_status ON test_apis(status);
CREATE INDEX IF NOT EXISTS idx_test_apis_gender ON test_apis(gender);
CREATE INDEX IF NOT EXISTS idx_test_apis_deleted ON test_apis(deleted);
CREATE INDEX IF NOT EXISTS idx_test_apis_created_at ON test_apis(created_at);
