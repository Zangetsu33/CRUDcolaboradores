import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;

public class Formulario extends JFrame {
    private Connection con;
    private PreparedStatement ps;
    private Statement st;
    private ResultSet r;
    private DefaultListModel<String> mod = new DefaultListModel<>();

    private JTextField IdText;
    private JTextField IDText;
    private JTextField CedulaText;
    private JTextField NombreText;
    private JTextField ApellidoText;
    private JTextField CargoText;
    private JButton ingresarBt;
    private JPanel panel;
    private JList<String> lista;
    private JButton consultarBt;
    private JButton searchById;
    private JTextField cedulaText;
    private JButton searchByCedula;

    public Formulario() {
        consultarBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    listar();
                    fetchDataFromServer("http://localhost:3000/personal/db");
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        ingresarBt.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ingresar();
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });

        searchById.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarPorID();
            }
        });

        searchByCedula.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buscarPorCedula();
            }
        });
    }

    private void buscarPorID() {
        try {
            int id = Integer.parseInt(IDText.getText());
            System.out.println("Buscando colaborador con ID: " + id);
            fetchDataFromServer("http://localhost:3000/personal/db/id/" + id);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Por favor, ingrese un ID válido.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void buscarPorCedula() {
        try {
            int cedula = Integer.parseInt(cedulaText.getText());
            System.out.println("Buscando colaborador con Cedula: " + cedula);
            fetchDataFromServer("http://localhost:3000/personal/db/cedula/" + cedula);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Por favor, ingrese una Cedula válida.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void listar() throws SQLException {
        listar("");
    }

    public void listar(String filter) throws SQLException {
        conectar();
        lista.setModel(mod);
        st = con.createStatement();
        String query = "SELECT id, cedula, nombre_completo, apellido_completo, cargo FROM db";
        if (!filter.isEmpty()) {
            query += " WHERE " + filter;
        }
        r = st.executeQuery(query);
        mod.removeAllElements();
        while (r.next()) {
            StringBuilder textoVertical = new StringBuilder();
            textoVertical.append("ID: ").append(r.getInt(1)).append("\n");
            textoVertical.append("Cédula: ").append(r.getInt(2)).append("\n");
            textoVertical.append("Nombre: ").append(r.getString(3)).append("\n");
            textoVertical.append("Apellido: ").append(r.getString(4)).append("\n");
            textoVertical.append("Cargo: ").append(r.getString(5));
            mod.addElement(textoVertical.toString());
        }
        st.close();
        con.close();
    }

    private void fetchDataFromServer(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            int status = con.getResponseCode();
            System.out.println("Response Code: " + status);

            if (status == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder content = new StringBuilder();
                while ((inputLine = in.readLine()) != null) {
                    content.append(inputLine).append("\n");
                }
                in.close();
                con.disconnect();

                System.out.println("Contenido recibido: \n" + content.toString());

                String jsonString = content.toString().trim();
                if (jsonString.startsWith("[")) { // Si es un array de objetos JSON
                    JSONArray jsonArray = new JSONArray(jsonString);
                    mod.removeAllElements();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);
                        String textoVertical = "<html>" +
                                "ID: " + obj.getInt("id") + "<br>" +
                                "Cédula: " + obj.getInt("cedula") + "<br>" +
                                "Nombre: " + obj.getString("nombre_completo") + "<br>" +
                                "Apellido: " + obj.getString("apellido_completo") + "<br>" +
                                "Cargo: " + obj.getString("cargo") +
                                "</html>";
                        mod.addElement(textoVertical);
                    }
                    lista.setModel(mod);
                } else if (jsonString.startsWith("{")) { // Si es un objeto JSON
                    JSONObject obj = new JSONObject(jsonString);
                    String textoVertical = "<html>" +
                            "ID: " + obj.getInt("id") + "<br>" +
                            "Cédula: " + obj.getInt("cedula") + "<br>" +
                            "Nombre: " + obj.getString("nombre_completo") + "<br>" +
                            "Apellido: " + obj.getString("apellido_completo") + "<br>" +
                            "Cargo: " + obj.getString("cargo") +
                            "</html>";
                    mod.removeAllElements();
                    mod.addElement(textoVertical);
                    lista.setModel(mod);
                } else {
                    mod.removeAllElements();
                    mod.addElement("<html>" + jsonString + "</html>");
                    lista.setModel(mod);
                }
            } else {
                throw new RuntimeException("Error en la conexión al servidor: " + status);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Error al obtener datos del servidor.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void ingresar() throws SQLException {
        String id = IdText.getText();
        String cedula = CedulaText.getText();
        String nombre_completo = NombreText.getText();
        String apellido_completo = ApellidoText.getText();
        String cargo = CargoText.getText();

        if (id.isEmpty() || cedula.isEmpty() || nombre_completo.isEmpty() || apellido_completo.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Todos los campos son obligatorios.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        conectar();
        try {
            if (idExists(id)) {
                JOptionPane.showMessageDialog(null, "El ID ya está siendo utilizado. Por favor, escoja uno nuevo.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if ((cargo.equalsIgnoreCase("gerente") || cargo.equalsIgnoreCase("ejecutivo")) && idWithCargoExists(id, cargo)) {
                JOptionPane.showMessageDialog(null, "El ID con cargo de 'gerente' o 'ejecutivo' ya está en uso.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            insertData(id, cedula, nombre_completo, apellido_completo, cargo);
        } catch (SQLException e) {
            if (e instanceof SQLIntegrityConstraintViolationException) {
                JOptionPane.showMessageDialog(null, "El ID ya existe en la base de datos.", "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new RuntimeException(e);
            }
        } finally {
            if (ps != null) ps.close();
            if (con != null) con.close();
        }
    }

    private void insertData(String id, String cedula, String nombre_completo, String apellido_completo, String cargo) throws SQLException {
        ps = con.prepareStatement("INSERT INTO db VALUES(?,?,?,?,?)");
        ps.setInt(1, Integer.parseInt(id));
        ps.setInt(2, Integer.parseInt(cedula));
        ps.setString(3, nombre_completo);
        ps.setString(4, apellido_completo);
        ps.setString(5, cargo);

        int result = ps.executeUpdate();
        if (result > 0) {
            lista.setModel(mod);
            mod.removeAllElements();
            mod.addElement("¡Se creó un nuevo colaborador!");
            enviarPeticionPOST(id, cedula, nombre_completo, apellido_completo, cargo);
            limpiarCampos();
        }
    }

    private boolean idExists(String id) throws SQLException {
        PreparedStatement checkStmt = con.prepareStatement("SELECT COUNT(*) AS countId FROM db WHERE id = ?");
        checkStmt.setInt(1, Integer.parseInt(id));
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            int count = rs.getInt("countId");
            return count > 0;
        }
        return false;
    }

    private boolean idWithCargoExists(String id, String cargo) throws SQLException {
        PreparedStatement checkStmt = con.prepareStatement("SELECT COUNT(*) AS countId FROM db WHERE id = ? AND cargo = ?");
        checkStmt.setInt(1, Integer.parseInt(id));
        checkStmt.setString(2, cargo);
        ResultSet rs = checkStmt.executeQuery();
        if (rs.next()) {
            int count = rs.getInt("countId");
            return count > 0;
        }
        return false;
    }

    private void enviarPeticionPOST(String id, String cedula, String nombre_completo, String apellido_completo, String cargo) {
        try {
            String url = "http://localhost:3000/personal/db/create";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json; utf-8");
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);

            JSONObject jsonInput = new JSONObject();
            jsonInput.put("id", id);
            jsonInput.put("cedula", cedula);
            jsonInput.put("nombre_completo", nombre_completo);
            jsonInput.put("apellido_completo", apellido_completo);
            jsonInput.put("cargo", cargo);

            try (OutputStream os = con.getOutputStream()) {
                byte[] input = jsonInput.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = con.getResponseCode();
            System.out.println("POST Response Code :: " + responseCode);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream(), "utf-8"));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                System.out.println(response.toString());
            } else {
                System.out.println("POST request not worked");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        IdText.setText("");
        CedulaText.setText("");
        NombreText.setText("");
        ApellidoText.setText("");
        CargoText.setText("");
    }

    public void conectar() {
        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost/fotosintesis", "root", "");
        } catch (Exception e) {
            System.err.println("Error: " + e);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Formulario");
        frame.setContentPane(new Formulario().panel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}