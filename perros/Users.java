package perros;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;

class User {
    private int id;
    private String name;

    public static User load(int id) throws Exception {
        User user = null;
        Statement stmt = DAL.getConnection().createStatement();
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
        Statement stmt = DAL.getConnection().createStatement();
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
        Statement stmt = DAL.getConnection().createStatement();
        ResultSet res = stmt.executeQuery("call getproduct(" + id + ")");
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

    public int getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public double getPrice() { return price; }
}

class DAL {
    private static Connection connection;
    private static final String CONN_STRING =
        "jdbc:mysql://localhost/acme?user=newuser&password=password";

    public static Connection getConnection() {
        return connection;
    }

    public static void init() throws ClassNotFoundException, SQLException {
        Class.forName("com.mysql.jdbc.Driver");
        connection = DriverManager.getConnection(CONN_STRING);
    }
}

class UsersHandler {
    public UsersHandler() throws Exception {
        DAL.init();
    }

    @Get
    public void handle(Request request) throws Exception {
        int userId = Integer.parseInt(request.queryParams().get("user"));
        User user = User.load(userId);
        String result = "{ \"user\": \"" + user.getName() + "\", \"products\": [";
        for (Product product : user.loadProducts()) {
            result += " { \"id\": " + product.getId() + ", ";
            result += "   \"name\": \"" + product.getName() + "\", ";
            result += "   \"price\": " + product.getPrice() + " }, ";
        }
        result = result.substring(0, result.length() - 2);
        result += " ] }";
        request.finish(200, result);
    }
}
