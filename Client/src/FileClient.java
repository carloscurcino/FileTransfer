import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class FileClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;

    private static Socket socket;
    private static DataInputStream in;
    private static DataOutputStream out;

    private static JList<String> fileList;
    private static DefaultListModel<String> listModel;

    public static void main(String[] args) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

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
            out.writeUTF("list");
            int numFiles = in.readInt();
            List<String> files = new ArrayList<>();
            for (int i = 0; i < numFiles; i++) {
                String fileName = in.readUTF();
                files.add(fileName);
            }
            listModel.clear();
            for (String file : files) {
                listModel.addElement(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void downloadFile() {
        String selectedFile = fileList.getSelectedValue();
        if (selectedFile != null) {
            try {
                out.writeUTF("download");
                out.writeUTF(selectedFile);

                String response = in.readUTF();
                if (response.equals("exists")) {
                    JFileChooser fileChooser = new JFileChooser();
                    fileChooser.setSelectedFile(new File(selectedFile));
                    int returnVal = fileChooser.showSaveDialog(null);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File savedFile = fileChooser.getSelectedFile();
                        try (OutputStream fileOut = new FileOutputStream(savedFile)) {
                            long fileSize = in.readLong();
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            long totalBytesRead = 0;
                            while (totalBytesRead < fileSize) {
                                bytesRead = in.read(buffer, 0, (int) Math.min(1024, fileSize - totalBytesRead));
                                if (bytesRead == -1) {
                                    break;
                                }
                                fileOut.write(buffer, 0, bytesRead);
                                totalBytesRead += bytesRead;
                            }
                        }
                        JOptionPane.showMessageDialog(null, "File downloaded successfully.");
                    }
                } else if (response.equals("not_found")) {
                    JOptionPane.showMessageDialog(null, "File not found on the server.");
                }
            } catch (IOException e) {
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
                out.writeUTF("upload");
                out.writeUTF(selectedFile.getName());

                try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(selectedFile))) {
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = fileIn.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                    }
                }
                // Indique o término do envio de arquivo
                out.writeLong(-1); // -1 para indicar o fim do arquivo
                JOptionPane.showMessageDialog(null, "File uploaded successfully.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
