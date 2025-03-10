package Sass;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Noel
 */
public class DAO {
    
    private String DB_DRIVER;
    private String DB_CONNECTION;
    private String DB_USER;
    private String DB_PASSWORD;

    public DAO() {
    }

    public String getDB_DRIVER() {
        return DB_DRIVER;
    }

    public String getDB_CONNECTION() {
        return DB_CONNECTION;
    }

    public String getDB_USER() {
        return DB_USER;
    }

    public String getDB_PASSWORD() {
        return DB_PASSWORD;
    }

    public void setDB_DRIVER(String DB_DRIVER) {
        this.DB_DRIVER = DB_DRIVER;
    }

    public void setDB_CONNECTION(String DB_CONNECTION) {
        this.DB_CONNECTION = DB_CONNECTION;
    }

    public void setDB_USER(String DB_USER) {
        this.DB_USER = DB_USER;
    }

    public void setDB_PASSWORD(String DB_PASSWORD) {
        this.DB_PASSWORD = DB_PASSWORD;
    }
    
    public Connection getDBConnection() {
        Connection dbConnection = null;
        try {
                Class.forName(DB_DRIVER);
        } catch (ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
        try {
            dbConnection = DriverManager.getConnection(DB_CONNECTION, DB_USER,DB_PASSWORD);
            return dbConnection;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
        return dbConnection;
    }
    
    public List<String> getTables(String motor) throws SQLException{
         Connection con = getDBConnection();
         Statement stmt = con.createStatement();
         List<String> tablas = new ArrayList<>();
         ResultSet rs;
         String[] m = motor.split(",");
         switch(m[0]){
            case "sql": 
                rs = stmt.executeQuery("SELECT name FROM sys.tables ORDER BY name");
                while (rs.next()){
                    String s = rs.getString("name");
                    if (!"sysdiagrams".equals(s) && !s.equals("Aud" + (s.substring(3, s.length())))) tablas.add(s);
                }
                break;
            case "postgres": 
                rs = stmt.executeQuery("select schemaname||'.'||tablename as nombre from pg_tables where schemaname NOT IN ('pg_catalog', 'information_schema')");
                while (rs.next()){
                    String s = rs.getString("nombre");
                    if (!s.split("\\.")[1].equals("aud_" + (s.split("\\.")[1].substring(4, s.split("\\.")[1].length())))) tablas.add(s);
                }
                break;
            case "mysql": 
                rs = stmt.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = '"+m[1]+"' ORDER BY table_name");
                while (rs.next()){
                    String s = rs.getString("table_name");
                    if (!s.equals("aud_" + (s.substring(4, s.length())))) tablas.add(s);
                }
                break;   
         }
         
         stmt.close();
         con.close();
         return tablas;
    }
    
    public void crearFuncionesSql() throws SQLException
    {
        Connection con = getDBConnection();
        Statement stmt = con.createStatement();

        String sqlCampos =
            "CREATE FUNCTION fcampos (@tabla VARCHAR(250)) " +
            "RETURNS VARCHAR(MAX) AS " +
            "BEGIN " +
                "DECLARE @Nombre varchar(250), @tipo VARCHAR(50), @longitud VARCHAR(50), " +
                        "@valores varchar(MAX); " +
                "SET @valores = '' " +

                "DECLARE curServidor CURSOR FOR " +
                "SELECT C.name,T.name,C.max_length FROM sys.columns C " +
                "INNER JOIN sys.types T ON T.system_type_id=C.system_type_id " +
                "WHERE C.object_id = ( " +
                "SELECT TA.object_id FROM sys.tables TA WHERE TA.name = @tabla) " +
                "ORDER BY C.column_id " +

                "OPEN curServidor; FETCH curServidor INTO @Nombre,@tipo,@longitud; " +
                "WHILE (@@FETCH_STATUS = 0) " +
                "BEGIN " +
                    "IF @tipo IN ('varchar','char','numeric','nvarchar','nchar') " +
                        "IF @longitud = '-1' SET @valores += @Nombre + ' '+ @tipo +  '(MAX), '; " +
                        "ELSE SET @valores += @Nombre + ' '+ @tipo +  '('+ @longitud + '), '; " +
                    "ELSE SET @valores += @Nombre + ' '+  @tipo + ', '; " +
                    "FETCH NEXT FROM curServidor INTO  @Nombre,@tipo,@longitud; " +
                "END " +
                "CLOSE curServidor; DEALLOCATE curServidor; " +
                "SET @valores += 'UsuarioAccion varchar(30), FechaAccion datetime, AccionSql varchar(30)' " +
                "RETURN @valores; " +
            "END;";
        String sqlAuditoria =
            "CREATE PROC aud_trigger @tabla VARCHAR(250) " +
            "AS " +
                "DECLARE @campos varchar(MAX), @createTable VARCHAR(MAX), @trigger VARCHAR(MAX) " +
                "SET @campos =  dbo.fcampos(@tabla) " +
                "SET @createTable = 'CREATE TABLE Aud'+@tabla+' (' +@campos+ ')' " +
                "SET @trigger =  " +
                "'CREATE TRIGGER Trg'+@tabla+'Aud ON dbo.'+@tabla+' AFTER UPDATE, DELETE " +
                "AS " +
                    "IF EXISTS(SELECT * FROM INSERTED) AND EXISTS(SELECT * FROM DELETED) " +
                    "BEGIN " +
                        "INSERT INTO dbo.Aud'+@tabla+' " +
                        "SELECT *, SUSER_NAME(), GETDATE() , ''Modificado'' " +
                        "FROM DELETED " +
                    "END " +
                    "ELSE IF EXISTS(SELECT * FROM DELETED) " +
                    "BEGIN " +
                        "INSERT INTO dbo.Aud'+@tabla+' " +
                        "SELECT *, SUSER_NAME(), GETDATE() , ''Eliminado'' " +
                        "FROM DELETED " +
                    "END' " +
                "IF EXISTS (SELECT name FROM sys.tables WHERE name='Aud'+@tabla)  " +
                    "EXEC ('DROP TABLE Aud'+@tabla) " +
                "IF EXISTS (SELECT name FROM sys.triggers WHERE name='Trg'+@tabla+'Aud')  " +
                    "EXEC ('DROP TRIGGER Trg'+@tabla+'Aud') " +
                "EXEC (@createTable) " +
                "EXEC (@trigger)";
        try
        {
            stmt.executeUpdate(sqlCampos);
        }
        catch (SQLException e) { }
        try
        {
            stmt.executeUpdate(sqlAuditoria);
        }
        catch (SQLException e) { }
        stmt.close();
        con.close();
    }
    
    public void crearFuncionesPostgres(String schema) throws SQLException
    {
        Connection con = getDBConnection();
        Statement stmt = con.createStatement();

        String sqlCampos =
            "CREATE OR REPLACE FUNCTION "+schema+".fcampos (tabla VARCHAR(50)) " + 
            "RETURNS VARCHAR(65535) AS $$ " + 
            "DECLARE nombre varchar(50); DECLARE tipo VARCHAR(50);  " + 
            "DECLARE longitud VARCHAR(50); DECLARE valores VARCHAR(65535); " + 
            "DECLARE cur_servidor CURSOR FOR " + 
                    "select column_name, data_type, character_maximum_length " + 
                    "from information_schema.columns " + 
                    "where table_name = tabla " + 
                    "ORDER BY ordinal_position; " + 
            "BEGIN " + 
                    "valores = ''; " + 

                    "OPEN cur_servidor; FETCH cur_servidor INTO nombre, tipo, longitud; " + 
                    "LOOP " + 
                        "EXIT WHEN NOT FOUND; " + 
                        "IF tipo IN ('bit','bit varying','character varying','character') THEN " + 
                            "valores = valores || nombre || ' '|| tipo || '('|| longitud || '), '; " + 
                        "ELSE valores = valores || nombre || ' '||  tipo || ', '; END IF; " + 
                        "FETCH NEXT FROM cur_servidor INTO nombre, tipo, longitud; " + 
                    "END LOOP; " + 

                    "CLOSE cur_servidor; " + 
                    "valores = valores || 'usuario_accion varchar(30), fecha_accion timestamp, accion_sql varchar(30)'; " + 

                    "RETURN valores; " + 
            "END; " + 
            "$$ " + 
            "language plpgsql; "; 
        String sqlAuditoria =
            "CREATE OR REPLACE FUNCTION "+schema+".aud_trigger(tabla VARCHAR(50)) " + 
            "RETURNS VOID AS $$ " + 
            "DECLARE campos VARCHAR(65535); DECLARE drop_table VARCHAR(250); DECLARE create_table VARCHAR(65535); " + 
            "DECLARE create_function VARCHAR(65535); DECLARE drop_trigger VARCHAR(250); DECLARE create_trigger VARCHAR(65535); " + 
            "BEGIN " + 
                "campos = "+schema+".fcampos(tabla); " + 
                "drop_table = 'DROP TABLE IF EXISTS "+schema+".aud_'||tabla; " + 
                "create_table = 'CREATE TABLE "+schema+".aud_'||tabla||' (' ||campos|| ')'; " + 
                "create_function =  " + 
                    "'CREATE OR REPLACE FUNCTION "+schema+".'||tabla||'_aud() RETURNS TRIGGER AS $BODY$  " + 
                    "BEGIN " + 
                        "IF TG_OP = ''UPDATE'' THEN " + 
                            "INSERT INTO "+schema+".aud_'||tabla||' " + 
                            "SELECT OLD.*, SESSION_USER, NOW(), ''Modificado''; " + 
                        "END IF; " + 
                        "IF TG_OP = ''DELETE'' THEN " + 
                            "INSERT INTO "+schema+".aud_'||tabla||' " + 
                            "SELECT OLD.*, SESSION_USER, NOW(), ''Eliminado''; " + 
                        "END IF; " + 
                        "RETURN NEW; " + 
                    "END; " + 
                    "$BODY$ LANGUAGE plpgsql;'; " + 
                "drop_trigger = 'DROP TRIGGER IF EXISTS '||tabla||'_aud ON "+schema+".'||tabla; " + 
                "create_trigger =  " + 
                    "'CREATE TRIGGER '||tabla||'_aud " + 
                    "AFTER UPDATE OR DELETE ON "+schema+".'||tabla||' " + 
                    "FOR EACH ROW EXECUTE PROCEDURE "+schema+".'||tabla||'_aud();'; " + 
                "EXECUTE drop_table; " + 
                "EXECUTE create_table; " + 
                "EXECUTE create_function; " + 
                "EXECUTE drop_trigger; " + 
                "EXECUTE create_trigger; " + 
            "END; " + 
            "$$ " + 
            "LANGUAGE 'plpgsql'; ";
        try
        {
            stmt.executeUpdate(sqlCampos);
        }
        catch (SQLException e) { }
        try
        {
            stmt.executeUpdate(sqlAuditoria);
        }
        catch (SQLException e) { }
        stmt.close();
        con.close();
    }
    
    public void crearFuncionesMySql() throws SQLException
    {
        Connection con = getDBConnection();
        Statement stmt = con.createStatement();

        String sqlCampos =
            "DROP function if exists fcampos; " + 
            "CREATE FUNCTION fcampos (bd text, tabla text) " + 
            "RETURNS TEXT " + 
            "BEGIN " + 
                "declare nombre text; declare tipo text;  " + 
                "declare valores text; declare finished integer default 0; " + 
                "declare cur_servidor cursor for " + 
                    "select column_name,column_type " + 
                    "from information_schema.columns " + 
                    "where table_schema = bd and table_name = tabla " + 
                    "order by ordinal_position; " + 
                "declare continue handler for not found set finished = 1; " + 
                "set valores = ''; " + 
                "OPEN cur_servidor; FETCH cur_servidor INTO nombre,tipo; " + 
                "recorrer: LOOP " + 
                    "if finished then leave recorrer; end if; " + 
                    "set valores =  concat(valores, nombre,' ',tipo,', ');  " + 
                    "FETCH NEXT FROM cur_servidor INTO nombre,tipo; " + 
                "END LOOP recorrer; " + 
                "CLOSE cur_servidor; " + 
                "set valores = concat(valores,'usuario_accion text, fecha_accion datetime, accion_sql text'); " + 
                "RETURN valores; " + 
            "END; ";
        String sqlCampos2 =
            "DROP function if exists fcampos2; " + 
            "CREATE FUNCTION fcampos2 (bd text, tabla text, prefijo text) " + 
            "RETURNS TEXT " + 
            "BEGIN " + 
                "declare nombre text; " + 
                "declare valores text; declare finished integer default 0; " + 
                "declare cur_servidor cursor for " + 
                    "select column_name " + 
                    "from information_schema.columns " + 
                    "where table_schema = bd and table_name = tabla " + 
                    "order by ordinal_position; " + 
                "declare continue handler for not found set finished = 1; " + 
                "set valores = ''; " + 
                "OPEN cur_servidor; FETCH cur_servidor INTO nombre; " + 
                "recorrer: LOOP " + 
                    "if finished then leave recorrer; end if;  " + 
                    "set valores =  concat(valores, prefijo, '.', nombre, ', ');  " + 
                    "FETCH NEXT FROM cur_servidor INTO nombre; " + 
                "END LOOP recorrer; " + 
                "CLOSE cur_servidor; " + 
                "set valores = concat(valores,'substring_index(current_user(),''@'',1), now()');" + 
                "RETURN valores; " + 
            "END; ";
        String sqlAuditoria =
            "DROP procedure if exists curso.aud_trigger; " + 
            "CREATE PROCEDURE aud_trigger(bd text, tabla text) " + 
            "begin " + 
                "DECLARE campos text;  " + 
                "set campos = fcampos(bd, tabla); " + 
                "set @drop_table = concat('DROP TABLE IF EXISTS ',bd,'.aud_',tabla); " + 
                "set @create_table = concat('CREATE TABLE ',bd,'.aud_',tabla,' ( ',campos, ' )'); " + 
                "PREPARE stmt1 FROM @drop_table; " + 
                "PREPARE stmt2 FROM @create_table; " + 
                "EXECUTE stmt1; " + 
                "EXECUTE stmt2;  " + 
            "end; ";
        try
        {
            stmt.executeUpdate(sqlCampos);
            stmt.executeUpdate(sqlCampos2);
            stmt.executeUpdate(sqlAuditoria);
        }
        catch (SQLException e) { }
        finally {
            if(stmt != null) stmt.close();
            if(con != null) con.close();
        }
    }
    
    public void generarSql(String tabla) throws SQLException {
        try (Connection con = getDBConnection(); Statement stmt = con.createStatement()) {
            String sql = "EXEC dbo.aud_trigger @tabla = '" + tabla + "'";
            stmt.executeUpdate(sql);
        }
    }
    
    public void generarPostgres(String tabla) throws SQLException {
        try (Connection con = getDBConnection(); Statement stmt = con.createStatement()) {
            String sql = "select "+tabla.split("\\.")[0]+".aud_trigger('" + tabla.split("\\.")[1] + "')";
            stmt.executeQuery(sql);
        }
    }
    
    public void generarMySql(String bd, String tabla) throws SQLException {
        try (Connection con = getDBConnection(); Statement stmt = con.createStatement()) {
            String sql = "call "+bd+".aud_trigger('" + bd + "','" + tabla + "')";
            
            ResultSet rs;
            String campos = "";
            rs = stmt.executeQuery("SELECT "+bd+".fcampos2('"+bd+"','"+tabla+"','OLD');");
            while (rs.next()) campos = rs.getString(1);
            String trigger_update = String.format(
                    "DROP trigger IF exists %s.update_%s_aud; "+
                    "CREATE TRIGGER %s.update_%s_aud " +
                        "AFTER UPDATE ON %s.%s " +
                         "FOR EACH ROW BEGIN  " +
                            "INSERT INTO %s.aud_%s VALUES(%s, 'Modificado');  " +
                        "END;  ",bd,tabla,bd,tabla,bd,tabla,bd,tabla,campos);
            String trigger_delete = String.format(
                    "DROP trigger IF exists %s.delete_%s_aud; "+
                    "CREATE TRIGGER %s.delete_%s_aud " +
                        "AFTER DELETE ON %s.%s " +
                         "FOR EACH ROW BEGIN  " +
                            "INSERT INTO %s.aud_%s VALUES(%s, 'Eliminado');  " +
                        "END;  ",bd,tabla,bd,tabla,bd,tabla,bd,tabla,campos);
            stmt.executeQuery(sql);
            stmt.executeUpdate(trigger_update);
            stmt.executeUpdate(trigger_delete);
        }
    }
}
