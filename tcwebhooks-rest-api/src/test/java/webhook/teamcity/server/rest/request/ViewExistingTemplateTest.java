package webhook.teamcity.server.rest.request;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static webhook.teamcity.server.rest.request.TemplateRequest.API_TEMPLATES_URL;

import java.io.FileNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.LoggingFilter;

import webhook.teamcity.BuildStateEnum;
import webhook.teamcity.payload.WebHookPayloadManager;
import webhook.teamcity.payload.WebHookPayloadTemplate;
import webhook.teamcity.payload.WebHookTemplateManager;
import webhook.teamcity.payload.template.ElasticSearchXmlWebHookTemplate;
import webhook.teamcity.payload.template.FlowdockXmlWebHookTemplate;
import webhook.teamcity.payload.template.SlackComCompactXmlWebHookTemplate;
import webhook.teamcity.server.rest.model.template.Template;
import webhook.teamcity.server.rest.model.template.Template.WebHookTemplateStateRest;
import webhook.teamcity.server.rest.model.template.Templates;
import webhook.teamcity.settings.config.WebHookTemplateConfig;
import webhook.teamcity.settings.config.builder.WebHookTemplateConfigBuilder;
import webhook.teamcity.settings.entity.WebHookTemplateEntity;
import webhook.teamcity.settings.entity.WebHookTemplateEntity.WebHookTemplateItem;
import webhook.teamcity.settings.entity.WebHookTemplateEntity.WebHookTemplateState;
import webhook.teamcity.settings.entity.WebHookTemplateJaxHelper;
import webhook.teamcity.settings.entity.WebHookTemplates;

public class ViewExistingTemplateTest extends WebHookAbstractSpringAwareJerseyTest {
	
	@Autowired 
	WebHookTemplateManager webHookTemplateManager;
	
	@Autowired 
	WebHookPayloadManager webHookPayloadManager;
	
	@Autowired
	WebHookTemplateJaxHelper webHookTemplateJaxHelper;
	
	WebResource webResource;
	
	@Before 
	public void setup(){
    	webResource = resource();
    	webResource.addFilter(new LoggingFilter(System.out));
	}

    @Test
    public void testXmlTemplatesRequest() {
        WebResource webResource = resource();
        Templates responseMsg = webResource.path(API_TEMPLATES_URL).accept(MediaType.APPLICATION_XML_TYPE).get(Templates.class);
        assertTrue(responseMsg.count == 0);
    }
    
    @Test
    public void testJsonTemplatesRequest() {
        WebResource webResource = resource();
        Templates responseMsg = webResource.path(API_TEMPLATES_URL).accept(MediaType.APPLICATION_JSON_TYPE).get(Templates.class);
        assertTrue(responseMsg.count == 0);
    }
    
    @Test
    public void testJsonTemplatesRequestUsingRegisteredTemplate() throws FileNotFoundException, JAXBException {
    	
    	WebResource webResource = resource();
    	WebHookTemplates templatesList =  webHookTemplateJaxHelper.read("../tcwebhooks-core/src/test/resources/webhook-templates.xml");
    	WebHookTemplateConfig templateEntity = WebHookTemplateConfigBuilder.buildConfig(templatesList.getWebHookTemplateList().get(0));
    	webHookTemplateManager.registerTemplateFormatFromXmlConfig(templateEntity);
    	Templates responseMsg = webResource.path(API_TEMPLATES_URL).accept(MediaType.APPLICATION_JSON_TYPE).get(Templates.class);
    	
    	prettyPrint(responseMsg);
    	
    	assertEquals(1, (int)responseMsg.count);
    	assertEquals(1, responseMsg.getTemplates().size());
    }

    @Test
    public void testJsonTemplatesRequestUsingLotsOfRegisteredTemplates() throws FileNotFoundException, JAXBException {
     	
    	WebResource webResource = resource();
    	WebHookTemplates templatesList =  webHookTemplateJaxHelper.read("../tcwebhooks-core/src/test/resources/webhook-templates.xml");
    	for (WebHookTemplateEntity templateEntity : templatesList.getWebHookTemplateList()){
    		webHookTemplateManager.registerTemplateFormatFromXmlEntity(templateEntity);
    	}
    	Templates responseMsg = webResource.path(API_TEMPLATES_URL).accept(MediaType.APPLICATION_JSON_TYPE).get(Templates.class);
    	assertEquals(3, (int)responseMsg.count);
    	assertEquals(3, responseMsg.getTemplates().size());
    	
    	prettyPrint(responseMsg);
    }    
    
    @Test
    public void testJsonTemplatesRequestUsingLotsOfRegisteredTemplatesButOnlyReturnignOne() throws FileNotFoundException, JAXBException {
    	
    	WebResource webResource = resource();
    	WebHookTemplates templatesList =  webHookTemplateJaxHelper.read("../tcwebhooks-core/src/test/resources/webhook-templates.xml");
    	assertEquals("There should be 3 templates loaded from file", 3, templatesList.getWebHookTemplateList().size());
    	
    	for (WebHookTemplateEntity templateEntity : templatesList.getWebHookTemplateList()){
    		webHookTemplateManager.registerTemplateFormatFromXmlEntity(templateEntity);
    	}
    	
    	Template responseMsg = webResource.path(API_TEMPLATES_URL + "/id:testXMLtemplate").accept(MediaType.APPLICATION_JSON_TYPE).get(Template.class);
    	assertEquals(1, responseMsg.getTemplates().size());
    	assertEquals("testXMLtemplate", responseMsg.id);
    	
    	prettyPrint(responseMsg);
    }    
    
    @Test
    public void testJsonTemplatesRequestUsingElasticTemplate() throws FileNotFoundException, JAXBException {
    	
    	WebHookPayloadTemplate elastic = new ElasticSearchXmlWebHookTemplate(webHookTemplateManager, webHookPayloadManager, webHookTemplateJaxHelper);
    	elastic.register();
    	
    	Template responseMsg = webResource.path(API_TEMPLATES_URL + "/id:elasticsearch").accept(MediaType.APPLICATION_JSON_TYPE).get(Template.class);
    	assertEquals(1, responseMsg.getTemplates().size());
    	assertEquals("elasticsearch", responseMsg.id);
    	
    	prettyPrint(responseMsg);
    }  
    
    @Test
    public void testJsonTemplatesRequestTemplateContentUsingElasticTemplate() throws FileNotFoundException, JAXBException {
    	
    	WebHookPayloadTemplate elastic = new ElasticSearchXmlWebHookTemplate(webHookTemplateManager, webHookPayloadManager, webHookTemplateJaxHelper);
    	elastic.register();
    	
    	String responseMsg = webResource.path(API_TEMPLATES_URL + "/id:elasticsearch/templateItem/id:1/templateContent").accept(MediaType.TEXT_PLAIN_TYPE).get(String.class);
    	assertEquals(elastic.getTemplateForState(BuildStateEnum.BUILD_FIXED).getTemplateText(), responseMsg);
    	prettyPrint(responseMsg);
    	
    	responseMsg = webResource.path(API_TEMPLATES_URL + "/id:elasticsearch/templateItem/id:1/branchTemplateContent").accept(MediaType.TEXT_PLAIN_TYPE).get(String.class);
    	assertEquals(elastic.getBranchTemplateForState(BuildStateEnum.BUILD_FIXED).getTemplateText(), responseMsg);
    	prettyPrint(responseMsg);
    }  
    
    @Test
    public void testJsonTemplatesRequestTemplateItemUsingElasticTemplate() throws FileNotFoundException, JAXBException {
    	
    	WebHookPayloadTemplate elastic = new ElasticSearchXmlWebHookTemplate(webHookTemplateManager, webHookPayloadManager, webHookTemplateJaxHelper);
    	elastic.register();
    	
    	WebHookTemplateConfig.WebHookTemplateItem responseMsg = webResource.path(API_TEMPLATES_URL + "/id:elasticsearch/templateItem/id:1").accept(MediaType.APPLICATION_JSON_TYPE).get(WebHookTemplateConfig.WebHookTemplateItem.class);
    	for (WebHookTemplateItem templateItem : elastic.getAsEntity().getTemplates().getTemplates()) {
    		if (responseMsg.getId() == templateItem.getId()){
    			assertEquals(responseMsg.getTemplateText().getTemplateContent(), templateItem.getTemplateText().getTemplateContent());
    		}
    	}
    	prettyPrint(responseMsg);
    	
    }  
    
    @Test
    public void testJsonTemplatesRequestBuildStateUsingElasticTemplate() throws FileNotFoundException, JAXBException {
    	
    	WebHookPayloadTemplate elastic = new ElasticSearchXmlWebHookTemplate(webHookTemplateManager, webHookPayloadManager, webHookTemplateJaxHelper);
    	elastic.register();
    	
    	
    	for (WebHookTemplateItem item : elastic.getAsEntity().getTemplates().getTemplates()){
    		for (BuildStateEnum state : BuildStateEnum.getNotifyStates()){
    			WebHookTemplateStateRest responseMsg = webResource.path(API_TEMPLATES_URL + 
    																	"/id:" + elastic.getTemplateShortName() + 
    																	"/templateItem/id:" + item.getId() + 
    																	"/buildState/" + state.getShortName()
    													   )
    													  .accept(MediaType.APPLICATION_JSON_TYPE)
    													  .get(WebHookTemplateStateRest.class);
    	    	prettyPrint(responseMsg);
    	    	for (WebHookTemplateState templateState: item.getStates()){
    	    		if (templateState.getType().equals(state.getShortName())){
    	    			assertEquals(templateState.isEnabled(), responseMsg.isEnabled());
    	    			assertEquals(templateState.getType(), responseMsg.getType());
    	    		}
    	    	}
    		}
    	}
    }
    
	@Test
	public void testJsonTemplatesRequestBuildStateUsingFlowdockTemplate() throws FileNotFoundException, JAXBException {
		
		WebHookPayloadTemplate flowdock = new FlowdockXmlWebHookTemplate(webHookTemplateManager, webHookPayloadManager, webHookTemplateJaxHelper);
		flowdock.register();
		
		
		for (WebHookTemplateItem item : flowdock.getAsEntity().getTemplates().getTemplates()){
			for (BuildStateEnum state : BuildStateEnum.getNotifyStates()){
				WebHookTemplateStateRest responseMsg = webResource.path(API_TEMPLATES_URL + 
						"/id:" + flowdock.getTemplateShortName() + 
						"/templateItem/id:" + item.getId() + 
						"/buildState/" + state.getShortName()
						)
						.accept(MediaType.APPLICATION_JSON_TYPE)
						.get(WebHookTemplateStateRest.class);
				prettyPrint(responseMsg);
				for (WebHookTemplateState templateState: item.getStates()){
					if (templateState.getType().equals(state.getShortName())){
						assertEquals(templateState.isEnabled(), responseMsg.isEnabled());
						assertEquals(templateState.getType(), responseMsg.getType());
					}
				}
			}
		}
	}

    
    @Test
    public void testJsonTemplatesRequestUsingSlackCompactTemplate() throws FileNotFoundException, JAXBException {
    	
    	WebHookPayloadTemplate slackCompact = new SlackComCompactXmlWebHookTemplate(webHookTemplateManager, webHookPayloadManager, webHookTemplateJaxHelper);
    	slackCompact.register();
    	
    	Template responseMsg = webResource.path(API_TEMPLATES_URL + "/id:slack.com-compact").accept(MediaType.APPLICATION_JSON_TYPE).get(Template.class);
    	
    	assertEquals(2, responseMsg.getTemplates().size());
    	assertEquals("slack.com-compact", responseMsg.id);
    	prettyPrint(responseMsg);
    }  
    

}