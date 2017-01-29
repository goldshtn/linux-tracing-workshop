import java.sql.*;
import java.util.ArrayList;

class User {
        private int id;
        private String name;

        public static User load(int id) throws Exception {
                User user = null;
                Statement stmt = Databasey.getConnection().createStatement();
                ResultSet res = stmt.executeQuery(
                        "select * from users where id = " + id);
                if (res.next()) {
                        user = new User();
                        user.id = res.getInt("id");
                        user.name = res.getString("name");
                }
                res.close();
                stmt.close();
                return user;
        }

        public Iterable<Product> loadProducts() throws Exception {
                ArrayList<Product> products = new ArrayList<>();
                Statement stmt = Databasey.getConnection().createStatement();
                ResultSet res = stmt.executeQuery(
                        "select id from products where userid = " + id);
                while (res.next()) {
                        Product prod = Product.load(res.getInt("id"));
                        products.add(prod);
                }
                res.close();
                stmt.close();
                return products;
        }

        public int getId() { return id; }
        public String getName() { return name; }
}

class Product {
        private int id;
        private int userId;
        private String name;
        private String description;
        private double price;

        public static Product load(int id) throws Exception {
                Product product = null;
                Statement stmt = Databasey.getConnection().createStatement();
                ResultSet res = stmt.executeQuery(
                        "call getproduct(" + id + ")");
                if (res.next()) {
                        product = new Product();
                        product.id = res.getInt("id");
                        product.userId = res.getInt("userid");
                        product.name = res.getString("name");
                        product.description = res.getString("description");
                        product.price = res.getDouble("price");
                }
                res.close();
                stmt.close();
                return product;
        }
}

class Databasey {
        private static Connection connection;
        private static final String CONN_STRING =
                "jdbc:mysql://localhost/acme?user=newuser&password=password";

        public static Connection getConnection() {
                return connection;
        }

        public static void main(String[] args) throws Exception {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(CONN_STRING);
                while (true) {
                        for (int i = 0; i < 100; ++i) {
                                User user = User.load(i);
                                user.loadProducts();
                                System.out.println("Loaded for user " + i);
                        }
                }
        }
}
