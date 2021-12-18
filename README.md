# Trivial-File-Transfer-Protocol
#
# The client and server communicate via UDP.
#
# The server is multi-threaded, meaning that several clients may be communicating with it at once.
#
# To enable this, the server accepts the initial  packet in a conversation from the client through 
# its published port (69), but then switches to another port for the reply and all further 
# communications during that conversation. This way the server can keep client conversations separated.
# 
# The client has to know to switch ports to the new port when it receives the reply to its
# first packet. Except for the first packet to the server, each client has its own port for a conversation.
