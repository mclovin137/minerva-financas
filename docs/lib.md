# Dependências e serviços do projeto

| Nome | Categoria | Versão declarada | Versão resolvida | Finalidade | Status e fonte |
|---|---|---:|---:|---|---|
| org.springframework.boot:spring-boot-starter-web | backend | 4.1.1 (BOM) | 4.1.1 | APIs web e servidor embutido | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.boot:spring-boot-starter-jdbc | persistência | 4.1.1 (BOM) | 4.1.1 | Spring JDBC e transações | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.xerial:sqlite-jdbc | banco | 3.51.1.0 | 3.51.1.0 | Driver JDBC do SQLite embarcado | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.boot:spring-boot-starter-validation | backend | 4.1.1 (BOM) | 4.1.1 | Validação de entradas | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.boot:spring-boot-starter-test | testes | 4.1.1 (BOM) | 4.1.1 | Testes unitários e de contexto | declarada no `backend/pom.xml`; resolvida no Maven Central |
| org.springframework.security:spring-security-crypto | segurança | via BOM do Boot | 7.1.1 | `BCryptPasswordEncoder` para o hash das senhas do seed (FDD-003, D-C2) | declarada no `backend/pom.xml`; resolvida no Maven Central |
| react | frontend | ^19.2.0 | 19.2.x | Biblioteca de interface da aplicação web | declarada em `frontend/package.json`; resolvida no npm |
| react-dom | frontend | ^19.2.0 | 19.2.x | Renderização do React no DOM | declarada em `frontend/package.json`; resolvida no npm |
| vite | build frontend | ^7.1.0 | 7.3.x | Build e servidor de desenvolvimento com proxy para a API | declarada em `frontend/package.json`; resolvida no npm |
| @vitejs/plugin-react | build frontend | ^5.0.0 | 5.x | Suporte a JSX e fast refresh no Vite | declarada em `frontend/package.json`; resolvida no npm |
| typescript | build frontend | ^5.9.0 | 5.9.x | Verificação de tipos em modo estrito | declarada em `frontend/package.json`; resolvida no npm |
| @types/react, @types/react-dom | build frontend | ^19.2.0 | 19.2.x | Tipos do React para o TypeScript | declaradas em `frontend/package.json`; resolvidas no npm |

O plugin `org.springframework.boot:spring-boot-maven-plugin` é build tooling, não dependência de runtime; sua versão resolvida pelo parent é 4.1.1.

O `spring-security-crypto` é a biblioteca isolada de criptografia do Spring Security, e **não** o `spring-boot-starter-security`: o projeto precisa apenas do BCrypt, e o starter traria a cadeia de filtros inteira, com autoconfiguração que teria de ser desligada. `npm audit` na instalação do frontend reportou **0 vulnerabilidades**.
