package ext.roop.pdm;

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.springframework.context.Lifecycle;

import com.ptc.core.lwc.server.PersistableAdapter;
import com.ptc.core.meta.common.DisplayOperationIdentifier;
import com.ptc.core.meta.common.UpdateOperationIdentifier;
import com.ptc.netmarkets.util.misc.NetmarketURL;

import ext.roop.RoopProperties;
import wt.change2.ChangeActivity2;
import wt.change2.ChangeException2;
import wt.change2.ChangeHelper2;
import wt.change2.ChangeHelper2;
import wt.content.ContentHelper;
import wt.doc.WTDocument;
import wt.change2.WTChangeOrder2;
import wt.doc.WTDocumentHelper;
import wt.doc.WTDocumentMaster;
import wt.doc.WTDocumentUsageLink;
import wt.enterprise.EnterpriseHelper;
import wt.epm.EPMDocument;
import wt.fc.Persistable;
import wt.fc.PersistenceHelper;
import wt.fc.QueryResult;
import wt.fc.WTObject;
import wt.folder.Folder;
import wt.folder.FolderEntry;
import wt.folder.FolderHelper;
import wt.iba.value.IBAHolder;
import wt.inf.container.WTContainer;
import wt.inf.container.WTContainerHelper;
import wt.lifecycle.LifeCycleHelper;
import wt.lifecycle.LifeCycleTemplate;
import wt.lifecycle.LifeCycleTemplateReference;
import wt.lifecycle.State;
import wt.maturity.PromotionNotice;
import wt.part.WTPart;
import wt.pds.StatementSpec;
import wt.pom.PersistenceException;
import wt.projmgmt.admin.Project2;
import wt.query.QueryException;
import wt.query.QuerySpec;
import wt.query.SearchCondition;
import wt.session.SessionHelper;
import wt.session.SessionServerHelper;
import wt.util.WTContext;
import wt.util.WTException;
import wt.util.WTProperties;
import wt.util.WTPropertyVetoException;
import wt.vc.Iterated;
import wt.vc.VersionControlHelper;
import wt.vc.Versioned;
import wt.vc.wip.NonLatestCheckoutException;
import wt.vc.wip.WorkInProgressException;
import wt.vc.wip.WorkInProgressHelper;

public final class RFQHelper {
	private static final String IBA_DUE_DATE = "Due_Date";

	public static void changeLCState(String[] docNumber, String stateName) {
		try {
			int size = docNumber.length;
			for (int i = 0; i < size; i++) {
				WTDocument doc = SearchObject.getDocument(docNumber[i]);

				LifeCycleHelper.service.setLifeCycleState(doc, State.toState(stateName));
			}
		} catch (QueryException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		}
	}
	public static WTDocument getDocumentTemplate(String templateName, String orgName) throws WTException {
		QuerySpec qs = new QuerySpec(WTDocument.class);
		SearchCondition templateNameSC = new SearchCondition(WTDocument.class, "master>name", "=", templateName);
		qs.appendWhere(templateNameSC, new int[] { 0, 1 });

		WTContainer container = WTContainerHelper.service.getByPath("/wt.inf.container.OrgContainer=" + orgName)
				.getReferencedContainer();

		QueryResult qrTemplates = EnterpriseHelper.service.getTemplates(container, qs, null, true, true, true);

		if (qrTemplates.hasMoreElements()) {
			QueryResult latest = VersionControlHelper.service.allVersionsOf((WTDocument) qrTemplates.nextElement());
			if (latest.hasMoreElements()) {
				return (WTDocument) latest.nextElement();
			}
		}
		return null;
	}

	/**
	 * Changing the child document state as per the input.
	 * 
	 * @param subType
	 *            given subtype to change the state.
	 * 
	 * @param stateName
	 *            the state name where we need to change.
	 * 
	 * @param pbo
	 *            primary business object of RFQ Workflow.
	 */
	public static void changeChildState(String subType, String stateName, WTDocument pbo) {
		try {
			/*
			 * Fetching all child document.
			 */
			final QueryResult qr = WTDocumentHelper.service.getUsesWTDocuments(pbo, wt.doc.WTDocumentConfigSpec
					.newWTDocumentConfigSpec(wt.doc.WTDocumentStandardConfigSpec.newWTDocumentStandardConfigSpec()));
			Persistable[] per = null;
			WTDocument document = null;
			while (qr.hasMoreElements()) {
				per = (Persistable[]) qr.nextElement();
				document = (WTDocument) per[1];
				
				/*
				 * Checking the subtype as per the input.
				 */
				if (com.ptc.core.meta.common.TypeIdentifierHelper.getType(document).toString().contains(subType)) {
					State localState = State.toState(stateName);
					LifeCycleTemplate objectLifecycleTemplate = (LifeCycleTemplate) VersionControlHelper.service
							.getLatestIteration((Iterated) document.getLifeCycleTemplate().getObject(), true);

					if (LifeCycleHelper.service.isState(objectLifecycleTemplate, localState)) {

						/**
						 * End of New added code
						 */
						System.out.println("-------Inside isState Condition IF---------");
						LifeCycleHelper.service.setLifeCycleState(document, localState);
					}
				}
			}
		} catch (WTException except) {
			except.printStackTrace();
		}

	}
	public static Date getCurrentDueDateWTDocument(Persistable primaryBusinessObject) throws WTPropertyVetoException, ParseException
	{
		Date dueDate=new Date();
		
		WTDocument doc=(WTDocument)primaryBusinessObject;
		if(doc!=null)
		{
			
			dueDate = (Date) getIBAValue(doc, "Due_Date");
			if(dueDate.toString()!="")
			{
				
			}
			dueDate.compareTo(dueDate);
			//dueDate=new SimpleDateFormat("yyyy-MM-dd").parse(DueDate);
			//DueDate=sdf.format(dueDate);
			//DeadlineDate=new SimpleDateFormat("yyyy-MM-dd").parse(Deadline);
//			if (DeadlineDate.compareTo(ActualFinishDate_1) >=0){
//				ActivityStatus="On Time";
//				completedPPAP++;
//			}
		}
		return dueDate; 
		
	}

	public static String createChildDoc(WTDocument parentDoc, String nameAppender, String numberAppender,
			String docType, String template) {
		String url = "";
		Folder rfqFoldr = null;
		System.out.println("Create child doc executing..ParrendtDocName:" + parentDoc.getName() + " Name apender:"
				+ nameAppender + " numberAppender:" + numberAppender);
		
		try {
			WTDocument latestParentDoc = SearchObject.getDocument(numberAppender + parentDoc.getNumber());
			if (latestParentDoc == null) {
				System.out.println("got the document...Null");
				WTDocument persistedDoc = WTDocument.newWTDocument();
				WTDocument childDoc = WTDocument.newWTDocument();
				childDoc.setName(nameAppender + parentDoc.getName());
				childDoc.setNumber(numberAppender + parentDoc.getNumber());
				childDoc.setContainer(parentDoc.getContainer());

				wt.type.TypeDefinitionReference typeRef = wt.type.TypedUtilityServiceHelper.service
						.getTypeDefinitionReference(docType);
				
				childDoc.setTypeDefinitionReference(typeRef);

				//childDoc.setTypeDefinitionReference(RMSMethodCall.setType(docType));

				if (!"CMP-".equals(numberAppender)) {
					childDoc.setDomainRef(parentDoc.getDomainRef());

					WTDocument docTemplate = getDocumentTemplate(template,
							RoopProperties.connectToPropertiesFile().getProperty("roop.orgName"));
					latestParentDoc = (WTDocument) VersionControlHelper.service.getLatestIteration(parentDoc, true);

					persistedDoc = (WTDocument) PersistenceHelper.manager.save(childDoc);
					if (docTemplate != null) {
						System.out.println("Document template is not null");
						ContentHelper.service.copyContent(docTemplate, persistedDoc);
					} else {
						System.out.println("Document Template is NULL");
					}
				} else {
					latestParentDoc = (WTDocument) VersionControlHelper.service.getLatestIteration(parentDoc, true);
					persistedDoc = (WTDocument) PersistenceHelper.manager.save(childDoc);
				}

				if (nameAppender.equalsIgnoreCase("Supplier PPAP Document-")) {
					System.out.println("inside if----------");
					rfqFoldr = FolderHelper.service.getFolder("/Default/Supplier CAPA/Supplier PPAP",
							parentDoc.getContainerReference());
				} else {
					rfqFoldr = FolderHelper.service.getFolder("/Default/RFQ Docs", parentDoc.getContainerReference());
				}

				FolderHelper.service.changeFolder(persistedDoc, rfqFoldr);

				WTDocument workingCopy = (WTDocument) WorkInProgressHelper.service
						.checkout(latestParentDoc, WorkInProgressHelper.service.getCheckoutFolder(), null)
						.getWorkingCopy();
				WTDocumentUsageLink link = WTDocumentUsageLink.newWTDocumentUsageLink(workingCopy,
						(WTDocumentMaster) persistedDoc.getMaster());
				System.out.println("**Craeted Link :- " + link);
				PersistenceHelper.manager.save(link);
				WorkInProgressHelper.service.checkin(workingCopy, null);

				url = generateHtmlHyperlinkedUrl(persistedDoc);
				System.out.println("Generated URL :-" + url);
			} else {
				String checkRfqVal = (String) getIBAValue(parentDoc, "RFQ_Type");
				System.out.println("RFQ Type :- " + checkRfqVal);
				if (checkRfqVal.contains("ECN/Revision")) {
					reviseCommercialProposal(numberAppender + parentDoc.getNumber());
					WTDocument revisedDoc = SearchObject.getDocument(numberAppender + parentDoc.getNumber());
					url = generateHtmlHyperlinkedUrl(revisedDoc);
					System.out.println("Generated URL After Revised the document :-" + url);
				}else{
					url = generateHtmlHyperlinkedUrl(latestParentDoc);
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (PropertyVetoException e) {
			e.printStackTrace();
		}

		return url;
	}

	public static String generateHtmlHyperlinkedUrl(WTDocument doc) {
		String homeUrl = null;
		String port = null;
		try {
			homeUrl = WTProperties.getLocalProperties().getProperty("wt.rmi.server.hostname");
			port = WTProperties.getLocalProperties().getProperty("wt.webserver.port");
		} catch (IOException e) {
			e.printStackTrace();
		}
		String url = "http://" + homeUrl + ":" + port + "/Windchill/app/#ptc1/tcomp/infoPage?ContainerOid=OR:"
				+ doc.getContainerReference() + "&oid=" + "VR:wt.doc.WTDocument:" + doc.getBranchIdentifier();
		String htmlHyperlinkedUrl = "<a href=\"" + url + "\">" + doc.getNumber() + "</a>" + "   ,";
		return htmlHyperlinkedUrl;
	}

	public static boolean isDueDate2DaysAway(WTDocument doc) {
		boolean result = false;
		Date dueDate = (Date) getIBAValue(doc, "Due_Date");

		Date currentDate = new Date();

		int day = 86400000;

		long dayDiffInMillis = dueDate.getTime() - currentDate.getTime() + 86400000L;
		double dateDifference = dayDiffInMillis / day;

		if (dateDifference <= 2.0D) {
			result = true;
		}
		return result;
	}

	public static Object getIBAValue(IBAHolder objHolder, String ibaName) {
		Object ibaValue = null;
		try {
			PersistableAdapter objNormalized = new PersistableAdapter((Persistable) objHolder, null,
					WTContext.getContext().getLocale(), null);

			objNormalized.load(new String[] { ibaName });

			ibaValue = objNormalized.get(ibaName);
		} catch (WTException e) {
			e.printStackTrace();
		}
		return ibaValue;
	}

	public static void reviseDocChildren(WTDocument parent) {
		try {
			List children = SearchObject.getAllDocChildren(parent);
			Iterator iter = children.iterator();
			while (iter.hasNext()) {
				WTDocument doc = (WTDocument) iter.next();
				Versioned newVersion = VersionControlHelper.service.newVersion(doc);
				FolderHelper.assignLocation((FolderEntry) newVersion, FolderHelper.getFolder(doc));
				PersistenceHelper.manager.save(newVersion);
			}
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}

	public static void reviseExceptFSDoc(WTDocument parent) {
		try {
			List children = SearchObject.getAllDocChildren(parent);
			Iterator iter = children.iterator();
			while (iter.hasNext()) {
				WTDocument doc = (WTDocument) iter.next();
				if (!doc.getNumber().startsWith("FS -")) {
					Versioned newVersion = VersionControlHelper.service.newVersion(doc);
					FolderHelper.assignLocation((FolderEntry) newVersion, FolderHelper.getFolder(doc));
					PersistenceHelper.manager.save(newVersion);
					LifeCycleHelper.service.setLifeCycleState(doc, State.toState("INWORK"));
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}

	public static void reviseCommercialProposal(String cpDoc) {
		try {
			WTDocument cpDocObj = SearchObject.getDocument(cpDoc);
			Versioned newVersion = VersionControlHelper.service.newVersion(cpDocObj);
			FolderHelper.assignLocation((FolderEntry) newVersion, FolderHelper.getFolder(cpDocObj));
			PersistenceHelper.manager.save(newVersion);
		} catch (WTException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		}
	}

	public static String generateProjectPlanUrl(String projectName) throws WTException, IOException {
		String port = null;
		Project2 project = SearchObject.getProject(projectName);
		String homeUrl = WTProperties.getLocalProperties().getProperty("wt.rmi.server.hostname");
		port = WTProperties.getLocalProperties().getProperty("wt.webserver.port");
		String url = "http://" + homeUrl + ":" + port
				+ "/Windchill/app/#ptc1/project/view_plan?oid=OR%3Awt.projmgmt.admin.Project2%3A"
				+ project.getPersistInfo().getObjectIdentifier().getId() + "&u8=1";

		return "<a href=\"" + url + "\">" + project.getName() + "</a>";
	}

	public static void updateDocIBA(Persistable pbo, String attributeName, String attributeValue) {
		try {
			WTDocument doc = (WTDocument) pbo;
			doc = (WTDocument) WorkInProgressHelper.service
					.checkout(doc, WorkInProgressHelper.service.getCheckoutFolder(), "CheckoutNote.").getWorkingCopy();
			IBAHolder holder = doc;
			PersistableAdapter lwcObject = new PersistableAdapter((Persistable) holder, null, null,
					new UpdateOperationIdentifier());
			lwcObject.load(new String[] { attributeName });
			lwcObject.set(attributeName, attributeValue);
			lwcObject.apply();
			lwcObject.persist();
			WorkInProgressHelper.service.checkin(doc, "updated");
		} catch (NonLatestCheckoutException e) {
			e.printStackTrace();
		} catch (WorkInProgressException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		}
	}
	public static boolean checkProjectExist(String ProjectName)
	{
		try
		{
			System.out.println("getting project number:"+ProjectName);
			QuerySpec qs = new QuerySpec(Project2.class);
			qs.appendWhere(new SearchCondition(Project2.class, Project2.NAME, SearchCondition.EQUAL,
					ProjectName), new int[] { 0 });
			
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			return (qr.size() > 0);
		}catch(WTException e)
		{
			e.printStackTrace();
		}
		return false;
	}

	public static boolean checkPromotionRequestExist(String pRequestNumber) {

		try {
			QuerySpec qs = new QuerySpec(PromotionNotice.class);
			qs.appendWhere(new SearchCondition(PromotionNotice.class, PromotionNotice.NUMBER, SearchCondition.EQUAL,
					pRequestNumber), new int[] { 0 });
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			return (qr.size() > 0);
		} catch (WTException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static String generateTimePlanUrl(String timePlanName)
	{
		String prjLinkUrl="";
		try
		{
			//String URL = "";
			Project2 project2=null;
			
			QuerySpec qs=new QuerySpec(Project2.class);
			qs.appendWhere(new SearchCondition(Project2.class,Project2.NAME,SearchCondition.EQUAL,timePlanName),new int[]{0});
			QueryResult qr=PersistenceHelper.manager.find((StatementSpec)qs);
			System.out.println("Project Number is.."+qr.size());
			while(qr.hasMoreElements())
			{
				project2=(Project2)qr.nextElement();
				if(project2!=null)
				{
					com.ptc.netmarkets.model.NmOid oid=new com.ptc.netmarkets.model.NmOid("Project",project2.getPersistInfo().getObjectIdentifier());
					com.ptc.netmarkets.util.beans.NmURLFactoryBean bean = new com.ptc.netmarkets.util.beans.NmURLFactoryBean();
					String url = NetmarketURL.buildURL(bean, "object", "view", oid);
					prjLinkUrl = "<a href=\"" + url + "\">-"+project2.getProjectNumber()+"</a>";
					System.out.println("Got the URL:"+prjLinkUrl);
					return prjLinkUrl;
				}
				else
				{
					System.out.println("Getting null.....");
				}
			}
		}catch(Exception e)
		{
			
		}
		return prjLinkUrl;
	}
	public static String generatePRequestUrl(String pRequestNumber) {
		try {
			String URL = "";
			String htmlHyperlinkedUrl = "";
			PromotionNotice prnotice = null;
			QuerySpec qs = new QuerySpec(PromotionNotice.class);
			System.out.println("Promotion Number...:" + pRequestNumber);
			qs.appendWhere(new SearchCondition(PromotionNotice.class, PromotionNotice.NUMBER, SearchCondition.EQUAL,
					pRequestNumber), new int[] { 0 });
			QueryResult qr = PersistenceHelper.manager.find((StatementSpec) qs);
			System.out.println("Got the promotion object:" + qr.size());
			while (qr.hasMoreElements()) {
				prnotice = (PromotionNotice) qr.nextElement();
				if (prnotice != null) {
					com.ptc.netmarkets.model.NmOid oid = new com.ptc.netmarkets.model.NmOid("PromotionNotice",
							prnotice.getPersistInfo().getObjectIdentifier());
					com.ptc.netmarkets.util.beans.NmURLFactoryBean bean = new com.ptc.netmarkets.util.beans.NmURLFactoryBean();
					String url = NetmarketURL.buildURL(bean, "object", "view", oid);
					htmlHyperlinkedUrl = "<a href=\"" + url + "\">Promotion Request-" + prnotice.getNumber() + "</a>";
					System.out.println("Got the URL:" + htmlHyperlinkedUrl);
					return htmlHyperlinkedUrl;
				}
			}
		} catch (WTException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static void updateEPMDocIBA(Persistable pbo, String attributeName, Object attributeValue) {
		try {

			EPMDocument doc = (EPMDocument) pbo;
			final boolean chkAccess = SessionServerHelper.manager.isAccessEnforced();
			SessionServerHelper.manager.setAccessEnforced(false);
			// doc = (EPMDocument)WorkInProgressHelper.service.checkout(doc,
			// WorkInProgressHelper.service.getCheckoutFolder(),
			// "CheckoutNote.").getWorkingCopy();
			ROOPIBAHelper.updateIBAs(doc, attributeName, ((Timestamp) attributeValue).toString());
			wt.fc.PersistenceServerHelper.manager.update(doc);

			/*
			 * WorkInProgressHelper.service.checkin(doc,
			 * "Updated Released Date"); QueryResult qrLinks =
			 * PersistenceHelper.manager.navigate(doc,
			 * EPMBuildRule.BUILD_TARGET_ROLE,EPMBuildRule.class, false); while
			 * (qrLinks.hasMoreElements()) { EPMBuildRule link = (EPMBuildRule)
			 * qrLinks.nextElement(); WTPart wtPart = (WTPart)
			 * link.getRoleBObject();
			 * if(!WorkInProgressHelper.isWorkingCopy(wtPart) &&
			 * !WorkInProgressHelper.isCheckedOut(wtPart) &&
			 * !LockHelper.isLocked(wtPart)) wtPart = (WTPart)
			 * WorkInProgressHelper.service.checkout(wtPart,
			 * WorkInProgressHelper.service.getCheckoutFolder(),
			 * "CheckoutNote.").getWorkingCopy();
			 * WorkInProgressHelper.service.checkin(wtPart,""); }
			 */
			SessionServerHelper.manager.setAccessEnforced(chkAccess);
		} catch (NonLatestCheckoutException e) {
			e.printStackTrace();
		} catch (WorkInProgressException e) {
			e.printStackTrace();
		} catch (WTPropertyVetoException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public static void updateProjectIBA(Persistable pbo, String projectName) {
		try {
			Project2 project = SearchObject.getProject(projectName);
			IBAHolder holder = project;
			PersistableAdapter lwcObject = new PersistableAdapter((Persistable) holder, null, Locale.US,
					new UpdateOperationIdentifier());
			lwcObject.load(new String[] { "RFQ_Number" });
			String newRfqNo = ((WTDocument) pbo).getNumber();
			lwcObject.set("RFQ_Number", newRfqNo);
			lwcObject.apply();
			lwcObject.persist();
		} catch (WTException e) {
			e.printStackTrace();
		}
	}

	public static boolean checkDocIBA(Persistable pbo, String attributeName, String attributeValue) {
		boolean checkVal = false;
		try {
			System.out.println("Attribute Value :- " + checkVal);
			WTDocument doc = (WTDocument) pbo;

			IBAHolder holder = doc;
			PersistableAdapter lwcObject = new PersistableAdapter((Persistable) holder, null, Locale.US,
					new UpdateOperationIdentifier());
			lwcObject.load(new String[] { attributeName });
			String attrVal = (String) lwcObject.getAsString(attributeName);
			System.out.println("Attribute Value :- " + attrVal);
			if (attributeValue.contentEquals(attrVal)) {
				checkVal = true;
			}

		} catch (NonLatestCheckoutException e) {
			e.printStackTrace();
		} catch (WorkInProgressException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (WTException e) {
			e.printStackTrace();
		}
		System.out.println("Result Value :- " + checkVal);
		return checkVal;
	}

	/*
	 * public static void validateDueDate(WTObject obj, Date workflowDueDate)
	 * throws WTException { System.out.println("Inside validateDueDate");
	 * WTDocument docObj = null; PersistableAdapter adapter = null; String
	 * rFQDueDate = "";
	 * System.out.println("Workflow Due date--------"+workflowDueDate.toString()
	 * );
	 * 
	 *//**
		 * formatting UTC date to IST
		 * 
		 * @throws ParseException
		 *//*
		 * 
		 * DateFormat gmtFormat = new
		 * SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS"); TimeZone istTime =
		 * TimeZone.getTimeZone("IST"); gmtFormat.setTimeZone(istTime); String
		 * finalIstTime = gmtFormat.format(workflowDueDate);
		 * System.out.println("IST Time: " + finalIstTime);
		 * 
		 * if (obj instanceof WTDocument) {
		 * System.out.println("Instance of WTDocument"); docObj = (WTDocument)
		 * obj; System.out.println("Document Name:---------" +
		 * docObj.getName()); adapter = new PersistableAdapter(docObj, null,
		 * SessionHelper.getLocale(), new DisplayOperationIdentifier());
		 * adapter.load("Due_Date"); rFQDueDate = (String)
		 * adapter.getAsString("Due_Date");
		 * System.out.println("Due Date from RFQ Attribute------------" +
		 * rFQDueDate); System.out.println("Due Date From Workflow-----------" +
		 * workflowDueDate.toString()); if(rFQDueDate == null || rFQDueDate
		 * ==""){ throw new
		 * WTException("Due Date on RFQ is not available. Please Provide Due Date on RFQ"
		 * ); } String[] rFQDueDateArray = rFQDueDate.split(" "); rFQDueDate =
		 * rFQDueDateArray[0];
		 * System.out.println("RFQ Due Date after array split------------" +
		 * rFQDueDate);
		 * 
		 * String[] workflowDueDateArray = finalIstTime.split(" "); String
		 * workflowDueDateString = workflowDueDateArray[0];
		 * System.out.println("Workflow due date------------------" +
		 * workflowDueDateString);
		 * 
		 * if (!(rFQDueDate.equals(workflowDueDateString))) { throw new
		 * WTException(
		 * "Due Date on RFQ and Due Date Entered in Workflow Task does not Match. Please Make sure both dates are equal"
		 * ); } else { System.out.println("Dates are matching"); }
		 * 
		 * } else { throw new WTException("Object Not Instance of WTDocument");
		 * }
		 * 
		 * }
		 */

	
	public static void validateDueDate(WTObject obj, Date workflowDueDate) throws WTException, ParseException {
		System.out.println("Inside validateDueDate");
		WTDocument docObj = null;
		PersistableAdapter adapter = null;
		String rFQDueDate = "";
		System.out.println("Workflow Due date--------" + workflowDueDate.toString());

		/**
		 * formatting UTC date to IST
		 */

		DateFormat gmtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS");
		TimeZone istTime = TimeZone.getTimeZone("IST");
		gmtFormat.setTimeZone(istTime);
		String finalIstTime = gmtFormat.format(workflowDueDate);
		System.out.println("IST Time: " + finalIstTime);

		if (obj instanceof WTDocument) {
			System.out.println("Instance of WTDocument");
			docObj = (WTDocument) obj;
			System.out.println("Document Name:---------" + docObj.getName());
			adapter = new PersistableAdapter(docObj, null, SessionHelper.getLocale(), new DisplayOperationIdentifier());
			adapter.load("Due_Date");
			rFQDueDate = (String) adapter.getAsString("Due_Date");
			System.out.println("Due Date from RFQ Attribute------------" + rFQDueDate);
			System.out.println("Due Date From Workflow-----------" + workflowDueDate.toString());
			if (rFQDueDate == null || rFQDueDate == "") {
				throw new WTException("Due Date on RFQ is not available. Please Provide Due Date on RFQ");
			}
			String[] rFQDueDateArray = rFQDueDate.split(" ");
			rFQDueDate = rFQDueDateArray[0];
			System.out.println("RFQ Due Date after array split------------" + rFQDueDate);

			String[] workflowDueDateArray = finalIstTime.split(" ");
			String workflowDueDateString = workflowDueDateArray[0];
			System.out.println("Workflow due date------------------" + workflowDueDateString);

			SimpleDateFormat rFQDateFormattter = new SimpleDateFormat("yyyy-MM-dd");
			Date rFQDate = rFQDateFormattter.parse(rFQDueDate);
			System.out.println("RFQ Date------------------------------------------" + rFQDate);
			SimpleDateFormat workflowDateFormattter = new SimpleDateFormat("yyyy-MM-dd");
			Date workflowDate = workflowDateFormattter.parse(workflowDueDateString);
			System.out.println("Workflow Date------------------------------------------" + workflowDate);

			if (workflowDate.after(rFQDate)) {
				throw new WTException(
						"Due Date in RFQ Document is Before Due Date mentioned in Workflow. Please Enter Due date less than or equal to date on RFQ document");
			}

		} else {
			throw new WTException("Object Not Instance of WTDocument");
		}

	}
	
	
	public static Date getNextDateWTDocument(Persistable primaryBusinessObject) throws WTPropertyVetoException, ParseException
	{
		System.out.println("Inside getNextDateWTDocument");
		Date dueDate=new Date();
		
		WTDocument doc=(WTDocument)primaryBusinessObject;
		System.out.println("document object is ---------------"+doc);
		Date nxtDate = null;
		if(doc!=null)
		{
			
			dueDate = (Date) getIBAValue(doc, "Due_Date");
			System.out.println("due date from IBA value in getNextDateWTDocument is ---------------"+dueDate);
			
				  final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
				 
				  final Calendar calendar = Calendar.getInstance();
				  calendar.setTime(dueDate);
				  calendar.add(Calendar.DAY_OF_YEAR, 1);
				  String nextDate =  format.format(calendar.getTime()); 
				  SimpleDateFormat rFQDueDateFormattter = new SimpleDateFormat("yyyy-MM-dd");
				  nxtDate = rFQDueDateFormattter.parse(nextDate);
			if(dueDate.toString()!="")
			{
				
			}
			dueDate.compareTo(dueDate);
			//dueDate=new SimpleDateFormat("yyyy-MM-dd").parse(DueDate);
			//DueDate=sdf.format(dueDate);
			//DeadlineDate=new SimpleDateFormat("yyyy-MM-dd").parse(Deadline);
//			if (DeadlineDate.compareTo(ActualFinishDate_1) >=0){
//				ActivityStatus="On Time";
//				completedPPAP++;
//			}
		}
		System.out.println("dueDate in getNextDateWTDocument is-------------"+dueDate);
		return nxtDate;
		
	}

	//Proposed due date should be greater than due date

		public static boolean validateProposedDueDate(String dueDate, String proposedDueDate,Persistable primaryBusinessObject) throws WTException,ParseException, WTPropertyVetoException 
		{
			System.out.println("Inside validateProposedDueDate");
			Date dueDate1 = getNextDateWTDocument(primaryBusinessObject);
			//DateFormat gmtDateFormat = new SimpleDateFormat("yyyy-MM-dd");
			System.out.println("validateProposedDueDate------------dueDate1 is----------------------"+dueDate1 );
			dueDate = dueDate1.toString();
			
			if((dueDate!="")&&(proposedDueDate!=""))
			{
			
			System.out.println("dueDate is----------------"+dueDate);
			System.out.println("validateProposedDueDate---------------proposedDueDate is----------------"+proposedDueDate);
			
			
			/*String[] rFQDueDateArray1 = dueDate.split(" ");
			dueDate = rFQDueDateArray1[0]; 
			System.out.println("validateProposedDueDate---------------RFQ Due Date after array split------------" + dueDate);*/

			String[] proposedDueDateArray = proposedDueDate.split(" ");
			String proposedDueDateString = proposedDueDateArray[0];
			System.out.println("validateProposedDueDate---------------Proposed due date is------------------" + proposedDueDateString);
			
			
			/*SimpleDateFormat rFQDueDateFormattter = new SimpleDateFormat("yyyy-MM-dd");
			Date rFQDueDate = rFQDueDateFormattter.parse(dueDate);
			System.out.println("validateProposedDueDate--------------------rFQDueDate is -----------------"+rFQDueDate);*/
			
			SimpleDateFormat proposedDueDateFormattter = new SimpleDateFormat("dd/MM/yyyy");
			Date proposedDate = proposedDueDateFormattter.parse(proposedDueDateString);
			System.out.println("validateProposedDueDate-------------------proposedDate is -----------------"+proposedDate);
			
			if ((proposedDate.before(dueDate1)) || (proposedDate.equals(dueDate1))) {
				//throw new WTException("Proposed Due Date should be greater than present due date");
				return true;
			}
			}
			return false;
		}
	public void avoidSonarCompliance() {
	}

}
