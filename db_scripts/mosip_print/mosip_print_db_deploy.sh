
## Properties file
set -e
properties_file="$1"
echo `date "+%m/%d/%Y %H:%M:%S"` ": $properties_file"
if [ -f "$properties_file" ]
then
     echo `date "+%m/%d/%Y %H:%M:%S"` ": Property file \"$properties_file\" found."
    while IFS='=' read -r key value
    do
        key=$(echo $key | tr '.' '_')
         eval ${key}=\${value}
    done < "$properties_file"
else
     echo `date "+%m/%d/%Y %H:%M:%S"` ": Property file not found, Pass property file name as argument."
fi

## Terminate existing connections
echo "Terminating active connections"
CONN=$(PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -t -c "SELECT count(pg_terminate_backend(pg_stat_activity.pid)) FROM pg_stat_activity WHERE datname = '$MOSIP_DB_NAME' AND pid <> pg_backend_pid()";exit;)
echo "Terminated connections"

## Drop db and role
echo "Dropping DB"
PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -f ./mosip_print/mosip_print_drop_db.sql
echo "Dropping user"
PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -f ./mosip_print/mosip_print_drop_roles.sql

## Create DB
echo "Creating DB and tables"
PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -f ./mosip_print/mosip_print_db.sql
PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -f ./mosip_print/mosip_print_ddl_deploy.sql

## Create users
echo `date "+%m/%d/%Y %H:%M:%S"` ": Creating database users"
PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -f ./mosip_print/mosip_role_mspprintuser.sql -v dbuserpwd=\'$DBUSER_PWD\'
PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -f ./mosip_print/mosip_print_grants.sql

## Populate tables
if [ ${DML_FLAG} == 1 ]
then
    echo `date "+%m/%d/%Y %H:%M:%S"` ": Deploying DML for ${MOSIP_DB_NAME} database"
    PGPASSWORD=$SU_USER_PWD psql --username=$SU_USER --host=$DB_SERVERIP --port=$DB_PORT --dbname=$DEFAULT_DB_NAME -a -b -f ./mosip_print/mosip_print_dml_deploy.sql
fi
