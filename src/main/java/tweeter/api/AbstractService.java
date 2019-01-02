package tweeter.api;

import com.google.gson.Gson;
import org.slf4j.Logger;

public abstract class AbstractService {

    protected static Logger logger;
    protected static Gson gson = new Gson();

    public static final int SUCCESS = 200;
    public static final int CLIENT_ERROR = 400;
    public static final int SERVER_ERROR = 500;

}
