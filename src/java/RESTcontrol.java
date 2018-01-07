import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import pl.jrj.mdb.IMdbManager;

/**
 * @author Michał Śliwa
 */
@Path("/control")
public class RESTcontrol
{
    @Context
    private UriInfo context;

    public RESTcontrol()
    {
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getHtml()
    {
        return "<html><body><h1>Hello, World!!</body></h1></html>";
    }

    @PUT
    @Consumes(MediaType.TEXT_HTML)
    public void putHtml(String content)
    {
    }
}

/**
 * Singleton class, holds service values between calls
 * @author Michał Śliwa
 */
class CounterData
{
    //------------------------------------------
    private static final Logger LOGGER = 
                            Logger.getLogger(CounterData.class.getName());
    //------------------------------------------
    
    //static instance
    private static CounterData instance = null;
    
     /**
     * Returns single instance of CounterData class
     * Ensures only one instance is present at any given time
     * Thread safe
     * @return instance of CounterData class
     */
    public static CounterData getInstance()
    {
        //check if instance is null or created instance failed to register
        if (instance == null || instance.sessionId == -1)
        {
            //synchronized block for thread safety
            synchronized (CounterData.class)
            {
                //double check if instance is null 
                //or created instance failed to register
                if (instance == null || instance.sessionId == -1)
                    //create new instance
                    instance = new CounterData();
            }
        }
        //returns current instance
        return instance;
    }
    
    //my album number
    private final String ALBUM = "108222";
    //MdbManager deployment descriptor
    private final String DB_MANAGER = "java:global/mdb-project/MdbManager!"+
                                      "pl.jrj.mdb.IMdbManager";
    
    //current state of bean
    private ServiceState state; 
    //value counter
    private int counter;
    //error counter
    private int error;
    //session id
    private int sessionId = -1;
    
    private CounterData()
    {
        this.counter = 0;
        this.error = 0;
        this.state = ServiceState.STANDBY;
        try
        {
            //create context
            javax.naming.Context ctx = new InitialContext();
            //lookup for MdbManager
            IMdbManager dbManager = (IMdbManager)ctx.lookup(DB_MANAGER);
            
            //get new session id
            String s = dbManager.sessionId(ALBUM);
            //check if session id can be parsed to int
            if(canParseInt(s))
                this.sessionId = Integer.parseInt(s);
            else
                this.sessionId = -1;
            if(this.sessionId == -1)
                LOGGER.log(Level.SEVERE, "!RECIEVED INVALID SESSION ID!");
        }
        catch (NamingException ex)
        {
            //log any errors
            LOGGER.log(Level.SEVERE, ex.toString());
            //set sessionId to null, indicates broken instance
            this.sessionId = -1;
        }
    }
    
    /**
     * Checks if string can be parsed to int
     * @param val string to parse
     * @return true if can be parsed otherwise false
     */
    private Boolean canParseInt(String val)
    {
        try
        {
            //try to parse string to integer
            Integer.parseInt(val);
            //return true
            return true;
        }
        catch(NumberFormatException ex)
        {
            //on exception return false
            return false;
        }
    }
}

/**
 * Enum representing states of the web service
 * @author Michał Śliwa
 */
enum ServiceState
{
    STANDBY, COUNTING;
}
