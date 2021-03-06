import java.net.*;
/**
 * PacketChecker - A class to check the errors of a packet
 * @author H Rose
 * @author Guiseppe Giambanco
 * @author Seth Button-Mosher
 * @version 2205
 */

public class PacketChecker implements TFTPConstants {

   /**
      Gives us something to log when an error shows up
      didn't implement error 3, 5, 6, 7 since the sheet says we don't need to
   */
   public static String decipher(DatagramPacket pkt) {
      Packet packet = new Packet();
      packet.dissectPacket(pkt);
      String result = "";
      if (packet.getOpcode() > 0 && packet.getOpcode() < 6) {
         result = String.format("Opcode (%d)", packet.getOpcode()); 
      }
      if (packet.getNumber() > -1 && packet.getNumber() < 8) {
         switch (packet.getOpcode()) {
            case 1:
            case 2:
               String temp = String.format("\nFilename <%s> Mode <%s>", packet.getS1(), packet.getS2());
               result = result + temp;
               return result;
            case 3:
            case 4:
               result = result + "\nBlock#: (" + packet.getNumber() + ")";
               return result;
         }
      }
      return result;
   }
}