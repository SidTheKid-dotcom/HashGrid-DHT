class Triplet
{
    private String IP_ADDR;
    private int UDP_PORT;
    private int NODE_ID;

    public Triplet()
    {
        this.IP_ADDR = null;
        this.UDP_PORT = -1;
        this.NODE_ID = -1;
    }

    public Triplet(String IP_ADDR, int UDP_PORT, int NODE_ID)
    {
        this.IP_ADDR = IP_ADDR;
        this.UDP_PORT = UDP_PORT;
        this.NODE_ID = NODE_ID;
    }


    public String getIP_ADDR() { return IP_ADDR; }
    public int getUDP_PORT() { return UDP_PORT; }
    public int getNODE_ID() { return NODE_ID; }

    public void display()
    {
        System.out.println("\t"+IP_ADDR+" "+UDP_PORT+" "+NODE_ID);
    }
}