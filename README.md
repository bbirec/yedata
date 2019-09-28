# yedata
Aurora Serverless를 사용하고 싶었지만 VPC 제약이 있어, AWS 내부 서버를 써야만 사용 가능했다. PaaS 플랫폼(Heroku) 또는 Serverless(now.sh, Cloudflare worker)에서도 사용가능한 [Aurora Serverless Data API](https://aws.amazon.com/blogs/aws/new-data-api-for-amazon-aurora-serverless/)가 새로 추가되었다.

Aurora Serverless Data API를 [yesql](https://github.com/krisajenkins/yesql) 스타일로 사용할수 있게 해준다.

## Features

- `.sql`파일을 읽어서 함수로 만들어줌 (yesql과 동일)
- `java.sql.Timestamp`, `json`, `jsonb` 타입 지원

## Usage

### 심플 쿼리
```clojure
(require '[yedata.data :as yd])
(def conn (yd/create-conn access-key secret-key resource-arn secret-arn db-name))

(yd/execute "select * from users where id=:id" {:id 1} conn)
; => ({:id 1, :name "Hello"})
```

### yesql 처럼 사용
```clojure
(require '[yedata.core :refer [defqueries require-sql]])
(def conn (yd/create-conn access-key secret-key resource-arn secret-arn db-name))

(defqueries "sql/query.sql") ;; 현재 ns에 함수로 정의
(simple-select {:id 1} conn)
; => ({:id 1, :name "Hello"})

(require-sql "sql/query.sql" :as q) ;; q ns에 함수로 정의
(q/simple-select {:id 1} conn)
; => ({:id 1, :name "Hello"})
```

## TODO

- [ ] Transaction
- [ ] Batch insert, update
- [ ] Sql statement function doc 생성


## Aurora Serverless 설정
1. Secret Manager로 추가
2. IAM policy 추가

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "secretsmanager:GetSecretValue",
            "Resource": "{Secret ARN}"
        },
        {
            "Sid": "VisualEditor1",
            "Effect": "Allow",
            "Action": [
                "rds-data:BatchExecuteStatement",
                "rds-data:BeginTransaction",
                "rds-data:CommitTransaction",
                "rds-data:ExecuteStatement",
                "rds-data:RollbackTransaction"
            ],
            "Resource": "arn:aws:rds:{REGION}:{Account ID}:cluster:{DB cluster id}"
        }
    ]
}
```

자세한 내용은 https://docs.aws.amazon.com/AmazonRDS/latest/AuroraUserGuide/data-api.html 참조


## Reference
- https://github.com/jeremydaly/data-api-client
- https://github.com/krisajenkins/yesql
