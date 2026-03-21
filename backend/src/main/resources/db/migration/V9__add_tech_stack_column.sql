-- tech_stack 컬럼 추가 (nullable, 기존 데이터 호환)
ALTER TABLE interview ADD COLUMN tech_stack VARCHAR(30) NULL;
