import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileServer {
    private static final int PORT = 12345;
    private static final String FILE_DIRECTORY = "/home/computacao/Desktop/Code Tower/Carlos-Curcino/FileTransfer/Server/src/arquivos_servidor/";

    public static void main(String[] args) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is running and listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Runnable clientHandler = new ClientHandler(clientSocket);
                executorService.submit(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(clientSocket.getInputStream());
                    DataOutputStream out = new DataOutputStream(clientSocket.getOutputStream())) {
                while (true) {
                    String command = in.readUTF();
                    if (command.equals("list")) {
                        File dir = new File(FILE_DIRECTORY);
                        String[] files = dir.list();
                        out.writeInt(files.length);
                        for (String fileName : files) {
                            out.writeUTF(fileName);
                        }
                    } else if (command.equals("download")) {
                        String fileName = in.readUTF();
                        File file = new File(FILE_DIRECTORY + fileName);
                        if (file.exists()) {
                            out.writeUTF("exists");
                            try (InputStream fileIn = new FileInputStream(file)) {
                                long fileSize = file.length();
                                out.writeLong(fileSize);
                                byte[] buffer = new byte[1024];
                                int bytesRead;
                                while ((bytesRead = fileIn.read(buffer)) != -1) {
                                    out.write(buffer, 0, bytesRead);
                                }
                            }
                        } else {
                            out.writeUTF("not_found");
                        }
                    } else if (command.equals("upload")) {
                        String fileName = in.readUTF();
                        long fileSize = in.readLong(); // Leia o tamanho do arquivo

                        try (OutputStream fileOut = new FileOutputStream(FILE_DIRECTORY + fileName)) {
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
                        // Não é necessário mais o uso de out.writeInt(-1) ou out.writeLong(-1)
                    }

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
