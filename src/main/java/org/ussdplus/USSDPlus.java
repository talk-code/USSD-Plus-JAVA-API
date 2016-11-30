package org.ussdplus;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.Queue;

/**
 * @author Mário Júnior
 */
public class USSDPlus {

    private static CoreFilter coreFilter = null;
    private static MenuIndexer menuIndexer;
    private static WindowRenderer windowRenderer = null;

    public static void setMenuIndexer(MenuIndexer enumerator){

        menuIndexer = enumerator;

    }

    protected static MenuIndexer getMenuIndexer(){

        return menuIndexer;

    }

    public static void setWindowRenderer(WindowRenderer windowRenderer){

        USSDPlus.windowRenderer = windowRenderer;

    }

    public static WindowRenderer getWindowRenderer(){

        return windowRenderer;

    }

    static {

        coreFilter = new CoreFilter();
        menuIndexer = new DefaultMenuIndexer();
        windowRenderer = new DefaultWindowRenderer();

    }

    public static USSDResponse executeRequest(USSDApplication application, USSDRequest request){

        return executeRequest(application,request,null);

    }

    protected static USSDResponse executeRequest(USSDApplication application, USSDRequest request, USSDSession session){

        USSDResponse ussdResponse = new BaseUSSDResponse();

        request.setApplication(application);

        //The core filter was not added to the application yet
        if(!application.getFilters().contains(coreFilter))
            application.getFilters().add(coreFilter);


        //Create a chain of all the filters
        USSDFilteringChain chain = createFilteringChain(application);

        //Call the filters chain
        chain.proceed(request,session,ussdResponse);


        if(ussdResponse.getWindow().isForm())
            ussdResponse.setResponseType(ResponseType.FORM);
        else
            ussdResponse.setResponseType(ResponseType.MESSAGE);


        //Return the produced USSDResponse
        return ussdResponse;

    }




    private static USSDFilteringChain createFilteringChain(USSDApplication application){

        final Queue<USSDFilter> filters = new LinkedList<USSDFilter>();
        filters.addAll(application.getFilters());

        USSDFilteringChain chain = new USSDFilteringChain() {


            public void proceed(USSDRequest request, USSDSession session, USSDResponse response) {

                if(filters.size()>0)
                    filters.poll().doFilter(request,session,response, this);

            }

            public void appendFilter(USSDFilter filter) {

                filters.add(filter);

            }
        };

        return chain;

    }

    public static USSDApplication getApplicationFromXML(InputStream inputStream){

        //TODO: Implement
        return null;

    }

}