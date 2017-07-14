import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;
/**
 * Created by Dani on 09.11.2014.
 * Last Modified on 18.01.2015
 */
public final class WebServer {

    public static void main(String args[]) throws Exception {

        int port = 6789;
        ServerSocket welcomeSocket = new ServerSocket(port);        // Establish listen socket
        /**
         *  Der if-Block wird benoetigt um den Datei-Pfad zur mime.types per Kommandozeilen-Parameter zu bestimmen.
         *  Wenn beim Programm-Start keine oder falsche Parameter uebergeben wurden,
         *  wird aufforderungEingabe() aufgerufen.
         *  Andernfalls wird ein String erzeugt (Eingabestrings kombiniert), wenn die Eingabe mit -mime beginnt.
         *
         */
        if (args.length == 0 ) {
            aufforderungEingabe();
        } else if ("-mime".equals(args[0])) {
            StringBuilder builder = new StringBuilder();
            for(String s : args) { builder.append(s + " "); }
            String eingabe = builder.toString();

            stringToPath(eingabe);          // Methodenaufruf
        } else {
            System.out.println("Unbekannter Parameter: " + args[0]);
            aufforderungEingabe();
        }
        /**
         * Wenn eine Verbindungsanfrage ankommt erzeugen wir ein HttpRequest-Objekt und uebergeben dabei die Referenz zum
         * Socket-Objekt ueber welches unsere Vebindung zum Client erzeugt wird.
         *
         * Damit das HttpRequest-Objekt ankommende HTTP-Service-Anfragen in einem separaten Thread verarbeiten kann,
         * erzeugen wir ein neues Thread Objekt, welches wir die Referenz zum HttpRequest-Objekt uebergeben.
         * Anschliessend wird thread.start() aufgerufen.
         *
         * Nachdem ein neuer Thread erzeugt und gestartet wurde springt das Programm wieder an den Anfang vom
         * main-Thread, welcher dann wieder auf eine TCP-Verbindungsanfrage wartet, waehrend der neue Thread
         * weiterlaeuft.
         */
        while (true) {
            Socket connectionSocket = welcomeSocket.accept();

            HttpRequest request = new HttpRequest(connectionSocket);
            Thread thread = new Thread(request);
            thread.start();
        }
    }
    /**
     * Die Methode aufforderungEingabe erwartet eine Kommandozeileneingabe und uebergibt der pathFromString-Methode
     * die Kommandozeileneingabe als String.
     */
    public static void aufforderungEingabe(){

        System.out.println("Bitte geben Sie den Pfad zum mime.types File " +
                "ueber den Parameter '-mime pfad/filename' ein.");
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String eingabe = br.readLine();
            stringToPath(eingabe);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    /**
     * Die Methode pathFromString erhaelt von der aufforderungEingabe-Methode einen String.
     * Der String wird beim ersten Leerzeichen gesplittet. Der Pfad besteht dann aus dem zweiten Teilstring.
     * Wenn die Eingabe kein Leerzeichen enthaelt, wird
     * @param eingabe
     */
    public static void stringToPath(String eingabe) {
        String[] eingabeSplit = eingabe.split(" ", 2);

        if (eingabeSplit.length > 1) {
            System.out.println("Pfad " + eingabeSplit[1]);
            createHashMap(eingabeSplit[1]);         // Uebergibt createHashMap den String
        } else {
            System.out.println("Keine Eingabe oder Unbekannte Parameter-Syntaxn " +
                    "\n - Es wird im aktuellen Verzeichnis gesucht");
            createHashMap("./mime.types");        // Uebergibt createHashMap den String
            //aufforderungEingabe();     // Sonst mimepfad = aktuelles verzeichnis.
        }
    }

    // Wir brauchen eine Hashmap um den ContenType mit der Dateiendung zu verknuepfen.
    public static HashMap<String, String> hashMap = new HashMap<String, String>();
    /**
     * Die Methode liest von der mime.types datei zeilenweise mit dem BufferedReader ein.
     * Wir erstellen eine Hashmap mit den Schluesseln und den zugehoerigen Werten.
     * @throws Exception
     */
    public static void createHashMap(String string){
        String mimeTypesPfad = string;
        String mimeZeile = null;

        try {
            BufferedReader inputMimeData = new BufferedReader(new FileReader(mimeTypesPfad));
            // Es wird gelesen solange Zeilen vorhanden sind.
            while ((mimeZeile = inputMimeData.readLine()) != null) {
                // Wenn die Zeile nicht leer ist und nicht mit # anfaengt wird sie mit StringTokenizer gesplittet.
                if ( (mimeZeile.length() != 0) && (!mimeZeile.trim().startsWith("#")) ) {
                    StringTokenizer mimetokens = new StringTokenizer(mimeZeile);

                    if (mimetokens.countTokens() > 1) {
                        String content = mimetokens.nextToken();
                        //System.out.print(content + "-  ");  //DEBUG
                        while ( mimetokens.hasMoreTokens() ) {
                            String ending = mimetokens.nextToken();
                            // Setze die Eintraege in der Hashmap.
                            hashMap.put(ending, content);
                            //System.out.print(ending + " ");   //DEBUG
                        } //System.out.println();               //DEBUG
                    }
                }
            }
            inputMimeData.close();          //schliesst den BufferedReader Stream
        }catch (Exception e) {
            System.out.println(e);
            System.out.println("Der durch getPfad gegebene mime.types-Pfad ist: "+ mimeTypesPfad);
        }
    }
}
/**
 * Wir deklarieren zwei Varaiblen fuer die HttpRequest Klasse: CRLF und socket. CR und LF werden benoetigt um den
 * HTTP Spezifikationen zu entsprechen. Die Variable socket speichert eine Referenz zum Verbindungs-Socket, welche dem
 * Konstruktor uebergeben wird.
 */
final class HttpRequest implements Runnable {

    final static String CRLF = "\r\n";
    Socket socket;
    // Constructor
    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }
    /**
     * Um eine Instanz von der HttpRequest Klasse an den Konstruktor des Threads zu uebergeben, muessen wir das
     * Runnable-Interface implementieren.
     */
    public void run() {
        try {
            processHttpRequest();
        } catch (Exception e) {
            System.out.println("Exception bei processHttpRequest: "+ e);
        }
    }

    private void processHttpRequest() throws Exception {
        InputStream is = socket.getInputStream();                                   //Get reference to inputstream
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());       // Get reference to outputstream
        BufferedReader br = new BufferedReader(new InputStreamReader(is));          //wrap filters around inputstream
        FileInputStream fis = null;                                         // wird benoetigt um die Datei einzulesen
        /**
         * ReadLine() liest bis zur CRLF Sequenz. Die erste gelesene Zeile ist die HTTP Request Line. Danach folgt die
         * Header Line. Da wir nicht wissen aus wie vielen Zeilen die Header Line besteht, muessen wir eine
         * While-Schleife implementieren, welche terminiert wenn die Zeil leer ist.
         *
         * Um Aufgabe 3 zu erfuellen werden ausserdem noch die weitere Zeilen des Headers gespeichert.
         */
        String requestLine = br.readLine() ;        // Get and display the request line of the HTTP request message.
        System.out.println("\n" + requestLine);

        String headerLine = null;
        String user_agent = null;
        String connection_status = null;

        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
            if (headerLine.startsWith("User-Agent")){
                user_agent = headerLine;
                //String[] sp = headerLine.split(" ");          //Gibt nur den letzten Teilstring aus
                //user_agent = sp[sp.length-1];
                //System.out.println("USER AGENT: " + user_agent);      //DEBUG
            } else if (headerLine.startsWith("Connection")) {
                connection_status = headerLine;
            }
        }
        /**
         * Die Requestline wird gesplittet. Wir ueberspringen GET und Speichern den darauffolgenden String.
         * Durch den angefuegten Punkt wird realisiert dass im aktuellen Verzeichnis gesucht wird.
         * Die Response Message besteht aus drei Teilen: Status Line, Response Header und Entity Body.
         */
        String[] requestLineSplit = requestLine.split(" ");
        String fileName = "." + requestLineSplit[1];

        SimpleDateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ");          // Inet-Datumsformat

        String statusLine = null;
        String contentTypeLine = "Content-type: " + "text/html;charset=utf-8" + CRLF;
        String entityBody = null;
        // Entity Header Fields Aufgabe 3
        String allowLine = "Allow: " + "GET, HEAD" + CRLF;
        String expiresLine = "Expires: " + "Sun, 11 Jan 2015 23:59:00 GMT" + CRLF;
        String dateLine = "Date: " + formatter.format(new Date()) + " GMT" + CRLF;
        String lastmodifiedLine = "Last-Modified: " + "Sun, 18 Jan 2015 02:33:00 GMT" + CRLF;
        String serverLine = "Server: " + "Daniel Nguyen's WebServer-Projekt" + CRLF;
        String poweredbyLine = "X-Powered-By: " + "Java 8.0" + CRLF;
        //String dateLine = "Date: "+ new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss ").format(new Date()) +" GMT"+ CRLF;
        // Zusätzliche Info
        String ipAddress = socket.getInetAddress().getHostAddress();                // IP-Adresse

        boolean fileExists = true;
        boolean getRequest = true;
        boolean headRequest = true;

        //ueberprueft die Anfrage Methode.             #Welcher if Block ist eleganter?
        /*
        if (!requestLine.startsWith("GET") && !requestLine.startsWith("HEAD")) { //Wenn nicht GET und nicht HEAD
            System.out.println("\nRequest-Methode nicht implementiert");
            getRequest = false;
            headRequest = false;
        } else if (requestLine.startsWith("GET")) {                   // Wenn GET-Methode, setze headRequest auf false
            System.out.println("\nGET-Request accepted");
            headRequest = false;
        } else { //if (requestLine.startsWith("HEAD"))                  // Ansonsten setze getRequest auf false
            System.out.println("\nHEAD-Request accepted");              // Es handelt sich um ein HEAD-Request
            getRequest = false;
        }*/
        if (!requestLine.startsWith("GET")) {
            getRequest = false;                                         // Wenn nicht GET, dann get = false
            if (!requestLine.startsWith("HEAD")) {
                headRequest = false;                                    // Wenn nicht GET und nicht HEAD: beide = false
                System.out.println("\nRequest-Methode nicht implementiert");
            } else {
                System.out.println("\nHEAD-Request accepted");          // Es handelt sich um ein HEAD-Request
            }
        } else if (requestLine.startsWith("GET")) {
            headRequest = false;
            System.out.println("\nGET-Request accepted");               // Es handelt sich um ein GET-Request
        }
        /**
         * Wir oeffnen die Datei mit Hilfe des FileInputStream, um sie spaeter an den Client zu schicken.
         * Anstatt den Thread zu terminieren, setzen wir den Boolean fileExists auf false, falls die Datei
         * nicht gefunden wird.
         */
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
            if (getRequest || headRequest) {
                System.out.println("Es wird 404 Not Found gesendet | " + e);
            } else {
                System.out.println("Es wird 501 Not implemented gesendet | " + e);
            }
        }
        /**
         * Status Line und Response Header werden durch CRLF terminiert. Falls die Datei nicht gefunden wird oder
         * eine von GET verschiedene Request-Methode angewandt wurde, wird ein HTTP spezifischer Fehlercode erzeugt.
         * Im Entity Body wird ausserdem ein HTML Dokument gespeichert.
         */
        if (getRequest || headRequest) {
            if (fileExists) {
                statusLine = "HTTP/1.0 200 OK" + CRLF;
                contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
                System.out.println("Es wird 200 OK und die angeforderte Datei " + fileName + " gesendet");

            } else {
                statusLine = "HTTP/1.0 404 Not Found" + CRLF;
                entityBody =
                        "<HTML>" +
                        "<HEAD><TITLE> 404 Not Found </TITLE></HEAD>" +
                        "<BODY><div align=\"center\">" +
                            "<hr />" +
                                "<h1>" + statusLine + "</h1>" +
                            "<hr />" +
                            "<p>" +
                                "<span style=\"color:blue\">" + "CLIENT-INFO:" + "</span><br>" +
                                "Browser-IP: " + ipAddress + "<br />" +
                                user_agent + "<br /> " +
                                "Angeforderter Content-Type: " + contentType(fileName) + "<br />" +
                                connection_status +
                            "</p>" +
                            "<p>" +
                                "<span style=\"color:#CC0000\">" + "SERVER-INFO:" + "</span><br>" +
                                serverLine + "<br />" +
                                allowLine + "<br />" +
                                "Client-Zugriff-" + dateLine + "<br />" +
                                expiresLine + "<br />" +
                            "</p>" +
                                poweredbyLine +
                        "</div>" +
                        "</BODY>" +
                        "</HTML>";
            }
        } else {
            statusLine = "HTTP/1.0 501 Not Implemented" + CRLF;
            entityBody =
                    "<HTML>" +
                    "<HEAD><TITLE> 501 Not Implemented </TITLE></HEAD>" +
                    "<BODY><div align=\"center\">" +
                        "<hr />" +
                            "<h1>"+ statusLine + "</h1>" +
                        "<hr />" +
                        "<p>" +
                            "<span style=\"color:blue\">" + "CLIENT-INFO:" + "</span><br>" +
                            "Browser-IP: " + ipAddress + "<br />" +
                            user_agent + "<br /> " +
                            //"Angeforderter Content-Type: " + contentType(fileName) + "<br />" +
                            connection_status +
                        "</p>" +
                        "<p>" +
                            "<span style=\"color:#CC0000\">" + "SERVER-INFO:" + "</span><br>" +
                            serverLine + "<br />" +
                            allowLine + "<br />" +
                            "Client-Zugriff-" + dateLine + "<br />" +
                            expiresLine + "<br />" +
                        "</p>" +
                            poweredbyLine +
                    "</div>" +
                    "</BODY>" +
                    "</HTML>";
        }
        /**
         * Nun koennen wir die Status-Line und die Header-Line an den Client schicken, indem wir sie in den
         * Outputstream schreiben. Anschliessend schicken wir ein CRLF um den Header zu terminieren.
         * Wenn die Anfrage-Methode GET ist und fileExists, dann wird die Methode sendFile() aufgerufen.
         * Ist dies nicht der Fall; und ist headRequest = false, dann wird der entityBody in den Outpustream geschrieben.
         */
        os.writeBytes(statusLine);
        os.writeBytes(expiresLine);
        os.writeBytes(dateLine);
        os.writeBytes(lastmodifiedLine);
        os.writeBytes(allowLine);
        os.writeBytes(serverLine);
        os.writeBytes(contentTypeLine);
        os.writeBytes(poweredbyLine);
        os.writeBytes(CRLF);

        if (getRequest && fileExists) {
            sendFile(fis, os);
            fis.close();
        } else if (headRequest) {
            System.out.println("Es wird kein entityBody gesendet");
        } else {
            os.writeBytes(entityBody);
        }

        os.close();         // close streams
        br.close();
        socket.close();     // close socket
    }
    /**
     * WIr konstruiren einen 1k Puffer um die Bytes zu senden.
     * sendFile erhaelt
     * @param fis
     * @param os
     * @throws Exception
     */
    private static void sendFile(FileInputStream fis, OutputStream os) throws Exception {
        try {
            byte[] puffer = new byte[1024];
            int bytes = 0;
            while ((bytes = fis.read(puffer)) != -1) {
                os.write(puffer, 0, bytes);
            }
        } catch(Exception e) {
            System.out.println("Fehler beim Puffern: " + e);
        }
    }

    private static String contentType(String fileName) throws Exception {
        StringTokenizer tokens = new StringTokenizer(fileName);
        // Wir springen zur Datei-Endung
        tokens.nextToken(".");
        // System.out.println(tokens.countTokens());
        if (tokens.countTokens()!=0) {
            String contentType = WebServer.hashMap.get(tokens.nextToken("."));
            return  contentType;
        }
        else {
            return "application/octet-stream";
        }
    }
}
