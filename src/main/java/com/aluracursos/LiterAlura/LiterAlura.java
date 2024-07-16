import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.util.Scanner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LiterAlura {
	private static final String API_URL = "https://gutendex.com/books/";
	private static final String DB_URL = "jdbc:sqlite:literAlura.db";

	public static void main(String[] args) {
		createDatabase();
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.println("Menú:");
			System.out.println("1. Buscar libro");
			System.out.println("2. Mostrar libros registrados");
			System.out.println("3. Buscar libros por idioma");
			System.out.println("4. Mostrar autores registrados");
			System.out.println("5. Buscar libros de mi año de nacimiento");
			System.out.println("6. Salir");
			int choice = scanner.nextInt();
			scanner.nextLine();  // Consume newline

			switch (choice) {
				case 1:
					System.out.print("Ingrese el término de búsqueda: ");
					String searchTerm = scanner.nextLine();
					searchBook(searchTerm);
					break;
				case 2:
					showRegisteredBooks();
					break;
				case 3:
					System.out.print("Ingrese el idioma (por ejemplo, 'en' para inglés): ");
					String language = scanner.nextLine();
					searchBooksByLanguage(language);
					break;
				case 4:
					showRegisteredAuthors();
					break;
				case 5:
					System.out.print("Ingrese su año de nacimiento: ");
					int birthYear = scanner.nextInt();
					searchBooksByYear(birthYear);
					break;
				case 6:
					System.out.println("¡Hasta luego!");
					return;
				default:
					System.out.println("Opción no válida, intente nuevamente.");
			}
		}
	}

	private static void createDatabase() {
		try (Connection conn = DriverManager.getConnection(DB_URL);
			 Statement stmt = conn.createStatement()) {
			String sql = "CREATE TABLE IF NOT EXISTS books (" +
					"id INTEGER PRIMARY KEY, " +
					"title TEXT NOT NULL, " +
					"authors TEXT, " +
					"languages TEXT, " +
					"download_url TEXT, " +
					"download_count INTEGER)";
			stmt.execute(sql);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void searchBook(String searchTerm) {
		try {
			String jsonResponse = get(API_URL + "?search=" + searchTerm);
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode booksNode = objectMapper.readTree(jsonResponse).path("results");
			for (JsonNode bookNode : booksNode) {
				int id = bookNode.path("id").asInt();
				String title = bookNode.path("title").asText();
				String authors = bookNode.path("authors").toString();
				String languages = bookNode.path("languages").toString();
				String downloadUrl = bookNode.path("formats").path("text/plain; charset=utf-8").asText();
				int downloadCount = bookNode.path("download_count").asInt();

				insertBook(id, title, authors, languages, downloadUrl, downloadCount);
				System.out.println("Título: " + title);
				System.out.println("Autores: " + authors);
				System.out.println("Idiomas: " + languages);
				System.out.println("URL de descarga: " + downloadUrl);
				System.out.println("Cantidad de descargas: " + downloadCount);
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void insertBook(int id, String title, String authors, String languages, String downloadUrl, int downloadCount) {
		String sql = "INSERT OR IGNORE INTO books (id, title, authors, languages, download_url, download_count) VALUES (?, ?, ?, ?, ?, ?)";
		try (Connection conn = DriverManager.getConnection(DB_URL);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setInt(1, id);
			pstmt.setString(2, title);
			pstmt.setString(3, authors);
			pstmt.setString(4, languages);
			pstmt.setString(5, downloadUrl);
			pstmt.setInt(6, downloadCount);
			pstmt.executeUpdate();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void showRegisteredBooks() {
		String sql = "SELECT id, title, authors, languages, download_url, download_count FROM books";
		try (Connection conn = DriverManager.getConnection(DB_URL);
			 Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				System.out.println("ID: " + rs.getInt("id"));
				System.out.println("Título: " + rs.getString("title"));
				System.out.println("Autores: " + rs.getString("authors"));
				System.out.println("Idiomas: " + rs.getString("languages"));
				System.out.println("URL de descarga: " + rs.getString("download_url"));
				System.out.println("Cantidad de descargas: " + rs.getInt("download_count"));
				System.out.println();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void searchBooksByLanguage(String language) {
		String sql = "SELECT id, title, authors, languages, download_url, download_count FROM books WHERE languages LIKE ?";
		try (Connection conn = DriverManager.getConnection(DB_URL);
			 PreparedStatement pstmt = conn.prepareStatement(sql)) {
			pstmt.setString(1, "%" + language + "%");
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				System.out.println("ID: " + rs.getInt("id"));
				System.out.println("Título: " + rs.getString("title"));
				System.out.println("Autores: " + rs.getString("authors"));
				System.out.println("Idiomas: " + rs.getString("languages"));
				System.out.println("URL de descarga: " + rs.getString("download_url"));
				System.out.println("Cantidad de descargas: " + rs.getInt("download_count"));
				System.out.println();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void showRegisteredAuthors() {
		String sql = "SELECT DISTINCT authors FROM books";
		try (Connection conn = DriverManager.getConnection(DB_URL);
			 Statement stmt = conn.createStatement();
			 ResultSet rs = stmt.executeQuery(sql)) {
			while (rs.next()) {
				System.out.println("Autores: " + rs.getString("authors"));
				System.out.println();
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}

	private static void searchBooksByYear(int year) {
		try {
			String jsonResponse = get(API_URL + "?publication_date_start=" + year + "-01-01&publication_date_end=" + year + "-12-31&sort=asc&page_size=5");
			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode booksNode = objectMapper.readTree(jsonResponse).path("results");
			for (JsonNode bookNode : booksNode) {
				int id = bookNode.path("id").asInt();
				String title = bookNode.path("title").asText();
				String authors = bookNode.path("authors").toString();
				String languages = bookNode.path("languages").toString();
				String downloadUrl = bookNode.path("formats").path("text/plain; charset=utf-8").asText();
				int downloadCount = bookNode.path("download_count").asInt();

				insertBook(id, title, authors, languages, downloadUrl, downloadCount);
				System.out.println("Título: " + title);
				System.out.println("Autores: " + authors);
				System.out.println("Idiomas: " + languages);
				System.out.println("URL de descarga: " + downloadUrl);
				System.out.println("Cantidad de descargas: " + downloadCount);
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String get(String urlStr) throws Exception {
		URL url = new URL(urlStr);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		int responseCode = conn.getResponseCode();

		if (responseCode == HttpURLConnection.HTTP_OK) {
			BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			return response.toString();
		} else {
			throw new Exception("GET request not worked");
		}
	}
}
