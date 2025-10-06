# Use microsoft SQL server image as the base
FROM mcr.microsoft.com/mssql/server:2022-latest

# Setting Environment Variables 
ENV ACCEPT_EULA=Y
ENV MSSQL_PID=Express
ENV MSSQL_SA_PASSWORD=Password1!

# Switch to root for directory creation and additional setups
USER root
RUN mkdir -p /app/scripts/migrations /app/scripts/seed

# Copy the initialization, migration, and seed scripts to the container
COPY init-database.sql /app/scripts/
COPY SQL/migrations/ /app/scripts/migrations/
COPY SQL/seed_data/ /app/scripts/seed/

# Install necessary tools
RUN apt-get update && apt-get install -y mssql-tools unixodbc-dev && apt-get clean

# Revert to default SQL Server user for security
USER mssql

EXPOSE 1433

# Healthcheck to ensure SQL Server is ready
HEALTHCHECK --interval=10s --timeout=5s --start-period=30s --retries=5 \
  CMD /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P $MSSQL_SA_PASSWORD -Q "SELECT 1" || exit 1

# Command to start SQL Server and execute the initialization script safely
CMD ["bash", "-c", "/opt/mssql/bin/sqlservr & \
  pid=$!; \
  echo 'Waiting for SQL Server to accept connections'; \
  for i in $(seq 1 30); do \
    /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P \"$MSSQL_SA_PASSWORD\" -Q 'SELECT 1' > /dev/null 2>&1 && break; \
    sleep 2; \
  done; \
    if ! /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P \"$MSSQL_SA_PASSWORD\" -Q \"SELECT name FROM sys.databases WHERE name = 'IdleLoadshedding'\" | grep -q IdleLoadshedding; then \
      echo 'Initializing IdleLoadshedding database'; \
      /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P \"$MSSQL_SA_PASSWORD\" -d master -i /app/scripts/init-database.sql; \
    else \
      echo 'Database already exists, running migrations and seed scripts'; \
    fi; \
    echo 'Applying schema migration script'; \
    /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P \"$MSSQL_SA_PASSWORD\" -d IdleLoadshedding -i /app/scripts/migrations/001_enhance_schema.sql; \
    echo 'Seeding baseline data'; \
    /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P \"$MSSQL_SA_PASSWORD\" -d IdleLoadshedding -i /app/scripts/seed/001_initial_data.sql; \
  wait $pid"]