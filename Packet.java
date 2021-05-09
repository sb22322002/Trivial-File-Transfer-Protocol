import java.io.*;
import java.net.*;

/**
 * Packet - A class to build and dissect DatagramPackets
 * @author H Rose
 * @author Guiseppe Giambanco
 * @author Seth Button-Mosher
 * @version 2205
 */
public class Packet implements TFTPConstants {
   // Attributes
   private int opcode;
   private int number;
   private String s1;
   private String s2;
   private byte[] data;
   private int dataLen;
   private InetAddress inet;
   private int port;
   
   /** Default constructor for Packet */
   public Packet(){
      opcode = 0;
      number = 0;
      s1 = null;
      s2 = null;
      data = null;
      dataLen = 0;
      inet = null;
      port = -1;
   }
   
   /**
    * Packet() - Parameterized constructor for Packet
    * @param   int            _opcode
    * @param   int            _number
    * @param   String         _s1
    * @param   String         _s2
    * @param   byte[]         _data
    * @param   int            _dataLen
    * @param   InetAddress    _inet
    * @param   int            _port
    */
   public Packet(int _opcode, int _number, String _s1, String _s2, byte[] _data, int _dataLen, InetAddress _inet, int _port) {
      opcode = _opcode;
      number = _number;
      s1 = _s1;
      s2 = _s2;
      data = _data;
      dataLen = _dataLen;
      inet = _inet;
      port = _port;
   }
   
   /** Accessor for opcode */
   public int getOpcode() {
      return opcode;
   }
   
   /** Accessor for error code number/block number */
   public int getNumber() {
      return number;
   }
   
   /** Accessor for String 1 (file name / error message) */
   public String getS1() {
      return s1;
   }
   
   /** Accessor for String 2 (mode) */
   public String getS2() {
      return s2;
   }
   
   /** Accessor for byte[] data */
   public byte[] getData() {
      return data;
   }
   
   /** Accessor for data length */
   public int getDataLen() {
      return dataLen;
   }
   
   /** Accessor for internet address */
   public InetAddress getInaPeer() {
      return inet;
   }
   
   /** Accessor for port number */
   public int getPort() {
      return port;
   }
   
   /** Mutator for S1 */
   public void setS1(String s) {
      s1 = s;
   }
   
   /** Mutator for S2 */
   public void setS2(String s) {
      s2 = s;
   }   
   /**
    * buildPacket()
    * method to build Packet
    * @return  DatagramPacket    pkt
    */
   public DatagramPacket buildPacket() {
      DatagramPacket pkt = null;
      String op = "";
      Exception except = null;
      
      // Switch on opcode
      switch (opcode) {
         case RRQ:
         case WRQ:
            try {
               int len = 2 + s1.length() + s2.length() + 2;
               ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
               DataOutputStream dos = new DataOutputStream(baos);
               
               // Write out RRQ/WRQ packet info
               dos.writeShort(opcode);
               dos.writeBytes(s1);
               dos.writeByte(0);
               dos.writeBytes(s2);
               dos.writeByte(0);
               dos.close();
               
               // Create DatagramPacket
               pkt = new DatagramPacket(baos.toByteArray(), UNDEF, (baos.toByteArray()).length, inet, port);
            }
            // General Exception...
            catch (IOException ioe) {
               op = "RRQ/WRQ";
               except = ioe;
            }
            break;
            
         case DATA:
            try {
               int len = 4 + data.length;
               ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
               DataOutputStream dos = new DataOutputStream(baos);
               
               // Write out DATA packet info
               dos.writeShort(opcode);
               dos.writeShort(number);
               
               if (dataLen > 0)
               dos.write(data, 0, dataLen); 
               
               dos.close();
               // Create DatagramPacket
               pkt = new DatagramPacket(baos.toByteArray(), UNDEF, (baos.toByteArray()).length, inet, port);
            }
            // IOException...
            catch (IOException ioe) {
               op = "DATA";
               except = ioe;
            }
            break;
            
         case ACK:
            try {
               int len = 4;
               ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
               DataOutputStream dos = new DataOutputStream(baos);
               
               // Write out ACK packet info
               dos.writeShort(opcode);
               dos.writeShort(number);
               dos.close();
               
               byte[] msg = baos.toByteArray();
               int msgLen = msg.length;
               // Create DatagramPacket
               pkt = new DatagramPacket(msg, msgLen, inet, port);
            }
            // IOException...
            catch (IOException ioe) {
               op = "ACK";
               except = ioe;
            }
            break;
            
         case ERROR:
            try {
               int len = 4 + s1.length() + 1;
               ByteArrayOutputStream baos = new ByteArrayOutputStream(len);
               DataOutputStream dos = new DataOutputStream(baos);
               
               // Write out EROOR packet info
               dos.writeShort(opcode);
               dos.writeShort(number);
               dos.writeBytes(s1);
               dos.writeByte(0);
               dos.close();
               
               // Create DatagramPacket
               pkt = new DatagramPacket(baos.toByteArray(), 0, (baos.toByteArray()).length, inet, port);
            }
            // IOException...
            catch (IOException ioe) {
               op = "DATA";
               except = ioe;
            }
            break;
            
         // Error message if opcode number is unknown
         default:
            System.out.println("buildPacket: Unknown opcode for packet: " + opcode + "\nFATAL!");
            System.exit(1);
            break;
      }
      // Check if exception is handled
      if (except != null) {
         String loc_op = op;
         Exception loc_except = except;
         System.out.println("buildPacket: Unexpected exception building " + loc_op + "\n" + loc_except);
      }
      return pkt;
   }
   
   /**
    * dissectPacket()
    * method to dissect Packet
    * @param   DatagramPacket    pkt
    */
   public void dissectPacket(DatagramPacket pkt) {
      ByteArrayInputStream bais = new ByteArrayInputStream(pkt.getData(), pkt.getOffset(), pkt.getLength());
      DataInputStream dis = new DataInputStream(bais);
      try {
         int nread;
         // Read in packet info
         opcode = dis.readShort();
         inet = pkt.getAddress();
         port = pkt.getPort();
         
         // Switch on opcode
         switch (opcode) {
            case RRQ:
            case WRQ:
               // Read in RRQ/WRQ file name and mode
               setS1(readToString(dis));
               setS2(readToString(dis));
               break;
            
            case DATA:
               // Read in DATA packet block code number
               number = dis.readShort();
               // Read in DATA packet length
               dataLen = pkt.getLength() - 4;
               data = new byte[dataLen];
               nread = dis.read(data, 0, dataLen);
               break;
            
            case ACK:
               // Read in ACK packet block code number
               number = dis.readShort();
               break;
            
            case ERROR:
               // Read in ERROR packet error code number and message
               number = dis.readShort();
               s1 = readToString(dis);
               break;
         }
      }
      // General Exception...
      catch (IOException ioe) {
         System.out.println("dissectPacket: Unexpected exception parsing packet: " + ioe);
         System.exit(3);
      }
      catch (Exception e) {
         System.out.println("dissectPacket: Unexpected exception parsing packet: " + e);
         System.exit(3);
      }
      // Try to close DataInputStream
      try {
         dis.close();
      }
      // General Exception...
      catch (Exception exception) {}
   }
   
   /**
    * readToString()
    * method to read in packet data and convert to String
    */
   private static String readToString(DataInputStream dis) throws Exception {
      String str = "";
      while (true) {
         int byt = dis.readByte();
         if (byt == 0) {
         return str; 
         }
         str = str + (char) byt;
      }
   }
}
