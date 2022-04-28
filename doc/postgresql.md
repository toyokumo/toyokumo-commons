# PostgreSQL-specific features

To use following features, `org.postgresql/postgresql` is required.

## toyokumo.commons.db.extension.postgresql
Provides extensions of reading objects that are in PostgreSQL from the `java.sql.ResultSet` or setting SQL parameters in statement objects.

See also [next.jdbc.date-time](https://github.com/seancorfield/next-jdbc/blob/develop/src/next/jdbc/date_time.clj), which provides default datetime extensions.

| Function/Var | Description |
| ------------ | ----------- |
| set-json-as-parameter | Make Clojure map jsonb in PreparedStatement |
| read-json | Read jsonb or json as Clojure data |

## toyokumo.commons.db.postgresql

| Function/Var | Description |
| ------------ | ----------- |
| \*format-table\* | How to format table name |
| \*format-column\* | How to format column name |
| copy-in | Use `COPY FROM STDIN` for very fast copying from a Reader into a database table |
