import com.sun.org.apache.regexp.internal.RE;
import com.sun.org.apache.regexp.internal.RESyntaxException;
import org.junit.Before;
import org.bantu.*;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Mário Júnior
 */
public class BasicApplicationTests {

    private static final String TEST_BASE_CODE ="*181#";

    private static USSDApplication getTestApp1(){

        USSDApplication application = new BaseUSSDApplication();

        //Login window
        Window loginWindow = new Window("login");
        loginWindow.addMessage(new Message("Please introduce your pin code","msg1"));
        loginWindow.addMenuItem(new MenuItem.Builder()
                .withDescription("Forgot password")
                .withTargetWindow("recoverPassword")
                .build());
        loginWindow.setInput(new Input.Builder().withName("pin").build());
        loginWindow.setProcessor(new USSDProcessor() {

            public void process(USSDRequest request, USSDSession session, USSDResponse response) {

                String pin = session.get("pin").toString();
                if(pin.equals("1234"))
                    request.redirectTo("operations", session, response);
                else
                    request.redirectTo("recoverPassword", session, response);





            }

        });

        //Recover password
        Window recoverPasswordWindow = new Window("recoverPassword");
        recoverPasswordWindow.addMessage(new Message("Ops! This is not working yet"));


        //Operations window
        Window operationsWindow = new Window("operations");
        operationsWindow.addMessage(new Message("Please select an operation","msg2"));
        operationsWindow.addMenuItem(new MenuItem.Builder()
                .withDescription("Transferences")
                .withTargetWindow("transferences")
                .build());
        operationsWindow.addMenuItem(new MenuItem.Builder()
                .withDescription("Withdrawal")
                .withTargetWindow("withdrawal")
                .build());



        //Transference window
        Window transferencesWindow = new Window("transferences");
        transferencesWindow.addMessage(new Message("Please select the origin account","msg3"));
        transferencesWindow.addMenuItemsProvider(new MenuItemsProvider() {

            public Collection<MenuItem> getMenuItems(String windowName, USSDRequest request, USSDSession session) {

                List<MenuItem> menuItemList = new ArrayList();
                menuItemList.add(new MenuItem.Builder()
                        .withDescription("3400231")
                        .withTargetWindow("amountWindow")
                        .build());


                menuItemList.add(new MenuItem.Builder()
                        .withDescription("171021")
                        .withTargetWindow("amountWindow")
                        .build());

                return menuItemList;


            }
        });




        //Amount window
        Window amountWindow = new Window("amountWindow");
        amountWindow.addMessage(new Message("Please introduce the amount"));
        amountWindow.setInput(new Input.Builder()
                .withName("amount")
                .withRegExp("[0-9]+","invalidAmount")
                .build());
        amountWindow.setProcessor(new USSDProcessor() {

            public void process(USSDRequest request, USSDSession session, USSDResponse response) {

                request.redirectTo("requestSubmitted",session,response);

            }
        });

        Window invalidAmountWindow = new Window("invalidAmount");
        invalidAmountWindow.addMessage(new Message("Invalid Amount : {{amount}}. Please try again"));


        Window requestSubmittedWindow = new Window("requestSubmitted");
        requestSubmittedWindow.addMessage(new Message("Request submitted successfully"));

        application.addWindow(loginWindow);
        application.addWindow(recoverPasswordWindow);
        application.addWindow(operationsWindow);
        application.addWindow(transferencesWindow);
        application.addWindow(amountWindow);
        application.addWindow(invalidAmountWindow);
        application.addWindow(requestSubmittedWindow);
        application.addWindow(recoverPasswordWindow);
        application.activateBaseCode(TEST_BASE_CODE);


        application.setStartupWindowId("login");
        return application;

    }



    @Before
    public void wipeSession(){

        USSDRequest request = getTestApp1().newRequest(TEST_BASE_CODE);
        fillRequestAndGetSession(request,getTestApp1()).close();

    }

    @Test
    public void mustRenderLoginWindow(){


        USSDRequest request = getTestApp1().newRequest(TEST_BASE_CODE);
        fillRequest(request);
        USSDResponse response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("login",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());

    }



    @Test
    public void mustRenderPasswordRecoverWindow(){

        USSDRequest request = getTestApp1().newRequest("1111");

        USSDApplication app = getTestApp1();
        fillRequestAndGetSession(request,app).setCurrentWindow("login");

        USSDResponse response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("recoverPassword",response.getWindow().getId());
        assertEquals(ResponseType.MESSAGE,response.getResponseType());

    }




    @Test
    public void mustRenderOperationsWindow(){

        USSDRequest request = getTestApp1().newRequest("1234");
        fillRequest(request);
        USSDResponse response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("operations",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());

    }



    @Test
    public void mustRenderInvalidAmountWindow(){

        USSDApplication app = getTestApp1();
        USSDRequest request = app.newRequest("1234");
        fillRequestAndGetSession(request,app).setCurrentWindow("login");

        USSDResponse response = BantU.executeRequest(app,request);
        assertEquals("operations",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());
        request = app.newRequest("1");
        fillRequest(request);

        response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("transferences",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());
        request = app.newRequest("2");
        fillRequest(request);

        response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("amountWindow",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());
        request = app.newRequest("-1000");
        fillRequest(request);

        response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("invalidAmount",response.getWindow().getId());
        assertEquals(ResponseType.MESSAGE,response.getResponseType());

    }


    @Test
    public void mustRenderRequestSubmittedWindow(){

        USSDApplication app = getTestApp1();
        USSDRequest request = app.newRequest("1234");
        fillRequest(request);

        USSDResponse response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("operations",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());
        request = app.newRequest("1");
        fillRequest(request);

        response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("transferences",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());
        request = app.newRequest("2");
        fillRequest(request);

        response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("amountWindow",response.getWindow().getId());
        assertEquals(ResponseType.FORM,response.getResponseType());
        request = app.newRequest("1000");
        fillRequest(request);

        response = BantU.executeRequest(getTestApp1(),request);
        assertEquals("requestSubmitted",response.getWindow().getId());
        assertEquals(ResponseType.MESSAGE,response.getResponseType());

    }


    @Test
    public void windowFilterMustBeInvoked(){

        USSDApplication application = new BaseUSSDApplication();
        Window window = new Window("startup");
        window.addMessage(new Message("Welcome, please type something"));
        window.setInput(new Input.Builder().withName("something").build());

        application.addWindowFilter("startup", new USSDFilter() {

            public void doFilter(USSDRequest request, USSDSession session, USSDResponse response, USSDFilteringChain chain) {

                chain.proceed(request,session,response);
                assertEquals("Added by the window filter",response.getWindow().getMessages().get(0).getContent());
                response.getWindow().getMessages().get(0).setContent("Modified by another window filter");

            }
        });

        application.addWindowFilter("startup", new USSDFilter() {

            public void doFilter(USSDRequest request, USSDSession session, USSDResponse response, USSDFilteringChain chain) {

                chain.proceed(request,session,response);
                response.getWindow().getMessages().get(0).setContent("Added by the window filter");

            }

        });

        application.addWindow(window);
        application.setStartupWindowId("startup");


        USSDRequest request = application.newRequest(TEST_BASE_CODE);
        fillRequest(request);
        USSDResponse response = BantU.executeRequest(application,request);
        assertEquals("Modified by another window filter",response.getWindow().getMessages().get(0).getContent());


    }




    @Test
    public void globalFilterMustBeInvoked(){

        USSDApplication application = new BaseUSSDApplication();
        Window mainWindow = new Window("main");
        mainWindow.addMessage(new Message("I'm the main window"));
        mainWindow.setInput(new Input.Builder().withName("something").build());

        Window secondWindow = new Window("second");
        secondWindow.addMessage(new Message("I'm the second window"));

        application.addFilter(new USSDFilter() {

            public void doFilter(USSDRequest request, USSDSession session, USSDResponse response, USSDFilteringChain execution) {

                session.setCurrentWindow("second");
                execution.proceed(request,session,response);

            }
        });

        application.addFilter(new USSDFilter() {

            public void doFilter(USSDRequest request, USSDSession session, USSDResponse response, USSDFilteringChain execution) {

                execution.proceed(request,session,response);
                response.getWindow().getMessages().get(0).setContent("I'm the third window");

            }
        });

        application.addWindow(mainWindow);
        application.addWindow(secondWindow);
        application.setStartupWindowId("main");

        USSDRequest request = application.newRequest(TEST_BASE_CODE);
        fillRequest(request);
        USSDResponse response = BantU.executeRequest(application,request);
        assertEquals("I'm the third window",response.getWindow().getMessages().get(0).getContent());

    }


    @Test(expected = WindowNotFoundException.class)
    public void exceptionMustBeThrownIfTargetWindowOfMenuCouldNotBeFound(){

        USSDApplication application = new BaseUSSDApplication();
        Window mainWindow = new Window("main");
        mainWindow.addMessage(new Message("Hello. Please select an option"));
        mainWindow.addMenuItem(
                new MenuItem.Builder().withDescription("First")
                .withTargetWindow("future-window")
                .build());

        mainWindow.addMenuItem(
                new MenuItem.Builder().withDescription("Second")
                        .withTargetWindow("present-window")
                        .build());


        application.addWindow(mainWindow);
        application.setStartupWindowId("main");

        USSDRequest request = application.newRequest("1");
        fillRequest(request);
        BantU.executeRequest(application,request);


    }

    @Test(expected = WindowNotFoundException.class)
    public void exceptionMustBeThrownIfStarupWindowCouldNotBeFound(){

        USSDApplication application = new BaseUSSDApplication();
        Window mainWindow = new Window("main");
        mainWindow.addMessage(new Message("This wont be rendered"));

        application.addWindow(mainWindow);
        application.setStartupWindowId("maniac");

        USSDRequest request = application.newRequest(TEST_BASE_CODE);
        fillRequest(request);
        BantU.executeRequest(application,request);


    }



    @Test
    public void regExpMustFailAndRequestRedirectToErrorWindow(){

        USSDApplication application = new BaseUSSDApplication();
        Window mainWindow = new Window("main");
        mainWindow.addMessage(new Message("Please type a value that matches the regular expression"));
        mainWindow.setInput(new Input.Builder().withName("age").withRegExp("[0-9]","error-window").build());


        Window errorWindow = new Window("error-window");
        errorWindow.addMessage(new Message("Regular expression failed","error"));


        application.addWindow(mainWindow);
        application.addWindow(errorWindow);

        application.setStartupWindowId("main");

        USSDRequest request = application.newRequest("ab");
        fillRequest(request);

        USSDResponse response = BantU.executeRequest(application,request);
        assertEquals("error-window",response.getWindow().getId());
        assertEquals("error",response.getWindow().getMessages().get(0).getId());

    }



    @Test
    public void regExpMustSucceedAndValueMustBeSavedInSession(){

        USSDApplication application = new BaseUSSDApplication();
        Window mainWindow = new Window("main");
        mainWindow.addMessage(new Message("Please type a value that matches the regular expression"));
        mainWindow.setInput(new Input.Builder().withName("age").withRegExp("[0-9]","error-window").build());

        application.addWindow(mainWindow);
        application.setStartupWindowId("main");

        USSDRequest request = application.newRequest("9");
        fillRequest(request);

        USSDResponse response = BantU.executeRequest(application,request);
        assertTrue(response.getSession().containsKey("age"));
        assertEquals("9",response.getSession().get("age").toString());

    }


    @Test
    public void inputValueWithoutRegExpMustBeFoundInSession(){

    }



    @Test
    public void previousWindowMustBeReturnedInResponse(){

        USSDApplication application = new BaseUSSDApplication();

        Window secondWindow = new Window("second");
        secondWindow.addMessage(new Message("This is just a non sense window"));
        secondWindow.addMenuItem(new MenuItem.Builder().withTargetWindow("#backward").withValue("0").build());

        Window firstWindow = new Window("first");
        firstWindow.addMessage(new Message("I'm the target of the backward menu item"));


        application.addWindow(secondWindow);
        application.addWindow(firstWindow);

        application.setStartupWindowId("second");

        USSDRequest request = application.newRequest("1");
        fillRequest(request);
        USSDSession session = application.getSessionProvider().getSession(request);
        session.setPreviousWindow("first");

        USSDResponse response = BantU.executeRequest(application,request);
        assertEquals("first",response.getWindow().getId());

    }

    @Test
    public void backwardNavigationMustBeSuccessfull(){

        USSDApplication application = new BaseUSSDApplication();

        Window thirdWindow = new Window("third");
        thirdWindow.addMessage(new Message("This is just a non sense window message"));
        thirdWindow.addMenuItem(new MenuItem.Builder().withTargetWindow("#backward").withValue("0").build());

        Window secondWindow = new Window("second");
        secondWindow.addMessage(new Message("This is just a non sense window"));
        secondWindow.addMenuItem(new MenuItem.Builder().withTargetWindow("third").build());

        Window firstWindow = new Window("first");
        firstWindow.addMessage(new Message("I'm the target of the backward menu item"));
        firstWindow.addMenuItem(new MenuItem.Builder().withTargetWindow("second").build());


        application.addWindow(thirdWindow);
        application.addWindow(secondWindow);
        application.addWindow(firstWindow);

        application.setStartupWindowId("first");
        USSDResponse response = null;

        USSDRequest request = application.newRequest("1");
        fillRequest(request);

        BantU.executeRequest(application,request); //window: second
        BantU.executeRequest(application,request); //window: third
        response = BantU.executeRequest(application,request); //window: second

        assertEquals("second",response.getWindow().getId());

    }


    @Test(expected = ImpossibleBackwardRedirectException.class)
    public void backwardRedirectMustFail(){

        USSDApplication application = new BaseUSSDApplication();

        Window uniqueWindow = new Window("unique");
        uniqueWindow.addMessage(new Message("This is just a non sense window message"));
        uniqueWindow.addMenuItem(new MenuItem.Builder().withTargetWindow("#backward").withValue("0").build());

        application.addWindow(uniqueWindow);

        application.setStartupWindowId("unique");
        USSDRequest request = application.newRequest("1");
        fillRequest(request);
        BantU.executeRequest(application,request);

    }


    @Test
    public void WindowMustBeFetchedFromNavigationCache(){

        NavigationCache navigationCache = new NavigationCache() {

            private Map<String,Window> items = new HashMap<String, Window>();

            public void storeWindow(Window window, USSDRequest request, USSDSession session) {

                items.put(window.getId(),window);

            }

            public Window fetchWindow(String windowId, USSDRequest request, USSDSession session) {

                return items.get(windowId);

            }
        };

        USSDApplication application = new BaseUSSDApplication();
        application.setNavigationCache(navigationCache);

        Window thanksWindow = new Window("thanks");
        thanksWindow.addMessage(new Message("Thanks your for picking a name"));
        application.addWindow(thanksWindow);


        Window exampleWindow = new Window("exampleWindow");
        exampleWindow.setInput(new Input.Builder().withName("value").build());
        exampleWindow.addMessage(new Message("Please select a name"));
        exampleWindow.addMenuItem(new MenuItem.Builder().withValue("Yman").withIndex("10").withTargetWindow("thanks").build());
        exampleWindow.addMenuItem(new MenuItem.Builder().withValue("Mario").withIndex("0").build());
        exampleWindow.addMenuItem(new MenuItem.Builder().withValue("Benjamin").withIndex("20").build());
        navigationCache.storeWindow(exampleWindow,null,null);


        application.setStartupWindowId("exampleWindow");
        USSDRequest request = application.newRequest("10");
        fillRequest(request);
        USSDResponse response = BantU.executeRequest(application,request);
        assertEquals("thanks",response.getWindow().getId());
        assertEquals(thanksWindow,response.getWindow());

    }

    @Test
    public void WindowMustBeStoredInNavigationCache(){

       final Map<String,Window> items = new HashMap<String, Window>();

        NavigationCache navigationCache = new NavigationCache() {

            public void storeWindow(Window window, USSDRequest request, USSDSession session) {

                items.put(window.getId(),window);

            }

            public Window fetchWindow(String windowId, USSDRequest request, USSDSession session) {

                return null;

            }
        };

        USSDApplication application = new BaseUSSDApplication();
        application.setNavigationCache(navigationCache);

        Window thanksWindow = new Window("thanks");
        thanksWindow.addMessage(new Message("Thanks your for picking a name"));
        thanksWindow.setInput(new Input.Builder().withName("type").build());
        application.addWindow(thanksWindow);


        Window exampleWindow = new Window("exampleWindow");
        exampleWindow.setInput(new Input.Builder().withName("value").build());
        exampleWindow.addMessage(new Message("Please select a name"));
        exampleWindow.addMenuItem(new MenuItem.Builder().withValue("Yman").withTargetWindow("thanks").build());
        exampleWindow.addMenuItem(new MenuItem.Builder().withValue("Mario").build());
        exampleWindow.addMenuItem(new MenuItem.Builder().withValue("Benjamin").build());
        application.addWindow(exampleWindow);

        application.setStartupWindowId("exampleWindow");
        USSDRequest request = application.newRequest("1");
        fillRequest(request);
        BantU.executeRequest(application,request);
        assertTrue(items.containsKey("thanks"));

    }


    @Test(expected = WindowFetchFailedException.class)
    public void FetchWindowFromNavigationCacheMustFail(){



    }

    @Test(expected = WindowStoreFailedException.class)
    public void StoredWindowInNavigationCacheMustFail(){



    }


    @Test
    public void SessionMustBeTerminatedIfUSSDResponseIsMessage(){




    }




    @Test
    public void messageMustBeRenderedWithSessionValues(){



    }

    private void fillRequest(USSDRequest request){

        request.setMSISDN("+258842538083");
        //TODO: Set more stuff here


    }

    private USSDSession fillRequestAndGetSession(USSDRequest request, USSDApplication app){

        fillRequest(request);
        return app.getSessionProvider().getSession(request);

    }



}
