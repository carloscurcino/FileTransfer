import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.List;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private static Socket socket;
    private static ObjectInputStream in;
    private static ObjectOutputStream out;

    private static JList<String> fileList;
    private static DefaultListModel<String> listModel;

    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            JFrame frame = new JFrame("File Client");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(400, 400);

            JPanel panel = new JPanel();
            JButton listButton = new JButton("List Files");
            JButton downloadButton = new JButton("Download File");
            JButton uploadButton = new JButton("Upload File");

            listModel = new DefaultListModel<>();
            fileList = new JList<>(listModel);
            fileList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            JScrollPane scrollPane = new JScrollPane(fileList);

            listButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    listFiles();
                }
            });

            downloadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    downloadFile();
                }
            });

            uploadButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    uploadFile();
                }
            });

            panel.add(listButton);
            panel.add(downloadButton);
            panel.add(uploadButton);

            frame.add(panel, BorderLayout.NORTH);
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.setVisible(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void listFiles() {
        try {
            out.writeObject("list");
            String[] files = (String[]) in.readObject();
            listModel.clear();
            for (String file : files) {
                listModel.addElement(file);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            try {
                out.writeObject("download");
                out.writeObject(selectedFile);

                String response = (String) in.readObject();
                if (response.equals("exists")) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(selectedFile));
                    int returnVal = fileChooser.showSaveDialog(null);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File savedFile = fileChooser.getSelectedFile();
                        try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(savedFile))) {
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = in.read(buffer)) != -1) {
                                fileOut.write(buffer, 0, bytesRead);
                            }
                        }
                        JOptionPane.showMessageDialog(null, "File downloaded successfully.");
                    }
                } else if (response.equals("not_found")) {
                    JOptionPane.showMessageDialog(null, "File not found on the server.");
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    private static void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                out.writeObject("upload");
                out.writeObject(selectedFile.getName());

                try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(selectedFile))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                // Indique o término do envio de arquivo
                out.writeObject("end");
                JOptionPane.showMessageDialog(null, "File uploaded successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
