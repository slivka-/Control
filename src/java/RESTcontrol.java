import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.CookieParam;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import pl.jrj.mdb.IMdbManager;

/**
 * @author Michał Śliwa
 */
@Path("/control")
public class RESTcontrol
{
    private final CounterData counterData = CounterData.getInstance(); 
    
    @Context
    private UriInfo context;

    public RESTcontrol()
    {
    }

    @GET
    @Path("/start")
    @Produces(MediaType.TEXT_PLAIN)
    public Response startCount(@CookieParam("msRestCookieId") Cookie userId)
    {
        if(userId == null)
        {
            return registerNewUser(true);
        }
        else
        {
            counterData.setUserCounting(userId.getValue());
            return Response.ok().build();
        }
    }
    
    @GET
    @Path("/stop")
    @Produces(MediaType.TEXT_PLAIN)
    public Response stopCount(@CookieParam("msRestCookieId") Cookie userId)
    {
        if(userId == null)
        {
            return registerNewUser(false);
        }
        else
        {
            counterData.setUserSuspended(userId.getValue());
            return Response.ok().build();
        }
    }
    
    @GET
    @Path("/clr")
    @Produces(MediaType.TEXT_PLAIN)
    public Response clrCounters(@CookieParam("msRestCookieId") Cookie userId)
    {
        if(userId == null)
        {
            return registerNewUser(false);
        }
        else
        {
            counterData.clearCounter(userId.getValue());
            return Response.ok().build();
        }
    }
    
    @GET
    @Path("/icr")
    @Produces(MediaType.TEXT_PLAIN)
    public Response incCounter(@CookieParam("msRestCookieId") Cookie userId)
    {
        if(userId == null)
        {
            return registerNewUser(false);
        }
        else
        {
            counterData.incrCounter(userId.getValue(),null);
            return Response.ok().build();
        }
    }
    
    @GET
    @Path("/icr/{num}")
    @Produces(MediaType.TEXT_PLAIN)
    public Response incCounterByNum(@CookieParam("msRestCookieId") Cookie userId, @PathParam("num") String num)
    {
        if(userId == null)
        {
            return registerNewUser(false);
        }
        else
        {
            counterData.incrCounter(userId.getValue(),num);
            return Response.ok().build();
        }
    }
    
    
    @GET
    @Path("/res")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getValues(@CookieParam("msRestCookieId") Cookie userId)
    {
        if(userId == null)
        {
            return registerNewUser(false);
        }
        else
        {
            return Response.ok().entity(counterData.getUserValues(userId.getValue())).build();
        }
    }
    
    @GET
    @Path("/err")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getErrors(@CookieParam("msRestCookieId") Cookie userId)
    {
        if(userId == null)
        {
            return registerNewUser(false);
        }
        else
        {
            return Response.ok().entity(counterData.getUserErrors(userId.getValue())).build();
        }
    }

    
    private Response registerNewUser(Boolean isCommandValid)
    {
        String newId = counterData.addNewUser(isCommandValid);
        return Response.ok().cookie(new NewCookie("msRestCookieId", newId)).build();
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
    //session id
    private int sessionId = -1;
    
    //collection of user information
    private HashMap<String,userState> usersStates;
    
    /**
     * private constructor
     */
    private CounterData()
    {
        usersStates = new HashMap<>();
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
     * Adds new user to counting service
     * @param isStart if user started with correct command
     * @return id of new user
     */
    public synchronized String addNewUser(Boolean isStart)
    {
        //generate new random id
        UUID newId = UUID.randomUUID();
        //create new state for user
        userState newState = new userState();
        //set counter to 0
        newState.counter = 0;
        //if started correctly
        if(isStart)
        {
            //0 errors, start counting
            newState.error = 0;
            newState.state = ServiceState.COUNTING;
        }
        else
        {
            //started with error
            newState.error = 1;
            newState.state = ServiceState.STANDBY;
        }
        //put new user to collection
        usersStates.put(newId.toString(), newState);
        return newId.toString();
    }
    
    /**
     * Set user to counting state
     * @param userId user id
     */
    public synchronized void setUserCounting(String userId)
    {
        //if user exists
        if(usersStates.containsKey(userId))
        {
            //get user info
            userState s = usersStates.get(userId);
            //if is standby set to counting , else err
            if(s.state == ServiceState.STANDBY)
                s.state = ServiceState.COUNTING;
            else
                s.error++;         
        }
    }
    
    /**
     * Set user to suspended state
     * @param userId user id
     */
    public synchronized void setUserSuspended(String userId)
    {
        //if user exists
        if(usersStates.containsKey(userId))
        {
            //get user info
            userState s = usersStates.get(userId);
            //if is counting set to standby, else err
            if(s.state == ServiceState.COUNTING)
                s.state = ServiceState.STANDBY;
            else
                s.error++;         
        }
    }
    
    /**
     * Clears users counter and error
     * @param userId user id
     */
    public synchronized void clearCounter(String userId)
    {
        //if user exists
        if(usersStates.containsKey(userId))
        {
            //get user info
            userState s = usersStates.get(userId);
            s.counter = 0;
            s.error = 0;
        }
    }
    
    /**
     * Increases counter value
     * @param userId
     * @param val 
     */
    public synchronized void incrCounter(String userId, String val)
    {
        //if user exists
        if(usersStates.containsKey(userId))
        {
            //get user info
            userState s = usersStates.get(userId);
            if(val == null)
                s.counter++;
            else
            {
                if(canParseInt(val))
                    s.counter += Integer.parseInt(val);
                else
                    s.error++;
            }
        }
    }
    
    /**
     * Get user counter val
     * @param userId
     * @return 
     */
    public synchronized String getUserValues(String userId)
    {
        //if user exists
        if(usersStates.containsKey(userId))
        {
            //get user info
            userState s = usersStates.get(userId);
            //check if counter value is more than 0
            if(s.counter > 0)
                //return modulo of sessionId by counter
                return String.valueOf(sessionId % s.counter);
        }
        return "ERROR";
    }
    
    /**
     * Get user error val
     * @param userId
     * @return 
     */
    public synchronized String getUserErrors(String userId)
    {
        //if user exists
        if(usersStates.containsKey(userId))
        {
            userState s = usersStates.get(userId);
            //check if counter value is more than 0
            if(s.error > 0)
                //return modulo of sessionId by error
                return String.valueOf(sessionId % s.error);
        }
        return "ERROR";
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
        
    /**
     * Enum representing states of single webservice user
     */
    private class userState
    {
        //current state of users counter
        public ServiceState state; 
        //value counter
        public int counter;
        //error counter
        public int error;
    }
            
    /**
    * Enum representing states of the web service
    */
    private enum ServiceState
    {
       STANDBY, COUNTING;
    }
}