package DB;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Set;

public class DatabaseService {
    Connection connection;
    public DatabaseService(List<Integer> initialIds) {
        try{
            this.connection = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/homework4?serverTimezone=Europe/Berlin","root","123");
            Statement stmt = this.connection.createStatement();
            String sql = "DROP TABLE Satellites";
            stmt.executeUpdate(sql);
            sql = "CREATE TABLE Satellites (SatelliteID int, errorCount varchar(255));";
            stmt.executeUpdate(sql);

            for(Integer id: initialIds) {
                sql = String.format("INSERT INTO Satellites VALUES (%d, 0)", id);
                stmt.executeUpdate(sql);
            }


        }
        catch(Exception e){
            System.out.println(e);
        }

    }

    public ResultSet executeStatement(String statement) {
        try {
            Statement stmt = this.connection.createStatement();
            ResultSet rs = stmt.executeQuery(statement);
            return rs;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    };

    public void manipulateData(String statement) {
        try {
            Statement stmt = this.connection.createStatement();
            stmt.executeUpdate(statement);

        } catch (Exception e) {
            e.printStackTrace();
        }
    };
}
