package gov.nih.nci.nbia;

import gov.nih.nci.cagrid.cqlquery.CQLQuery;
import gov.nih.nci.cagrid.cqlresultset.CQLQueryResults;
import gov.nih.nci.cagrid.data.utilities.CQLQueryResultsIterator;
import gov.nih.nci.cagrid.ncia.client.NCIACoreServiceClient;
import gov.nih.nci.ncia.domain.Series;
import java.io.InputStream;

import javax.xml.namespace.QName;

import org.globus.wsrf.encoding.ObjectDeserializer;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.xml.sax.InputSource;

public class NBIAGridClient {
	public static void main(String args[]) throws Exception {
		new NBIAGridClient().run();
	}
	public void run() throws Exception {
		String gridServiceUrl = "http://imaging.nci.nih.gov/wsrf/services/cagrid/NCIACoreService";
		NCIACoreServiceClient nciaClient = new NCIACoreServiceClient(gridServiceUrl);
		CQLQuery cqlQuery = loadXMLFile();
		processCQLResult(nciaClient.query(cqlQuery));
	}
	private void processCQLResult(CQLQueryResults results) throws Exception {
		if (results != null) {
			CQLQueryResultsIterator iter = new CQLQueryResultsIterator(results);
			while (iter.hasNext()) {
				Series obj = (Series) iter.next();
				if (obj == null) {
					System.out.println("something not right.  obj is null");
					continue;
				} else {
					System.out.println("Result series instance uid is "
							+ obj.getInstanceUID() + " modality: "
							+ obj.getModality());
				}
			}
		}
	}
	private CQLQuery loadXMLFile() throws Exception{
		CQLQuery newQuery = null;
			InputStream is = NBIAGridClient.class.getResourceAsStream("cqlQuery.txt");
			InputSource query = new InputSource(is);
			newQuery = (CQLQuery) ObjectDeserializer.deserialize(query, CQLQuery.class);
			System.out.println(ObjectSerializer.toString(newQuery,
					new QName("http://CQL.caBIG/1/gov.nih.nci.cagrid.CQLQuery", "CQLQuery")));		
		return newQuery;
	}
}
