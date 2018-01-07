import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.Produces;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PUT;
import javax.ws.rs.core.MediaType;

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
