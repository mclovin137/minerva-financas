# Dependências e serviços do projeto

| Nome | Categoria | Versão declarada | Versão resolvida | Finalidade | Status e fonte |
|---|---|---:|---:|---|---|
| org.springframework.boot:spring-boot-starter-web | backend | 4.1.1 (BOM) | 4.1.1 | APIs web e servidor embutido | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.boot:spring-boot-starter-jdbc | persistência | 4.1.1 (BOM) | 4.1.1 | Spring JDBC e transações | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.xerial:sqlite-jdbc | banco | 3.51.1.0 | 3.51.1.0 | Driver JDBC do SQLite embarcado | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.boot:spring-boot-starter-validation | backend | 4.1.1 (BOM) | 4.1.1 | Validação de entradas | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.boot:spring-boot-starter-test | testes | 4.1.1 (BOM) | 4.1.1 | Testes unitários e de contexto | declarada no `backend/pom.xml`; resolvida no Maven Central |

O plugin `org.springframework.boot:spring-boot-maven-plugin` é build tooling, não dependência de runtime; sua versão resolvida pelo parent é 4.1.1.
