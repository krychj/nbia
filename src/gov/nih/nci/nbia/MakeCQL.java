package gov.nih.nci.nbia;

import edu.emory.cci.ivi.helper.HashmapToCQLQuery;
import edu.emory.cci.ivi.helper.ModelMap;
import edu.emory.cci.ivi.helper.ModelMapException;
import gov.nih.nci.cagrid.cqlquery.CQLQuery;
import gov.nih.nci.cagrid.cqlquery.QueryModifier;
import gov.nih.nci.cagrid.data.MalformedQueryException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.globus.wsrf.encoding.ObjectSerializer;
import org.globus.wsrf.encoding.SerializationException;

import com.pixelmed.dicom.Attribute;
import com.pixelmed.dicom.AttributeList;
import com.pixelmed.dicom.AttributeTag;
import com.pixelmed.dicom.CodeStringAttribute;
import com.pixelmed.dicom.DicomDictionary;
import com.pixelmed.dicom.ShortStringAttribute;
import com.pixelmed.dicom.SpecificCharacterSet;
import com.pixelmed.dicom.TagFromName;
//MakeSQL Test.
public class MakeCQL {
	final static Logger logger = Logger.getLogger(MakeCQL.class);
	Properties prop = new Properties();
	Map<String, String> map;
	
	public MakeCQL(){
		
	}
	
	public CQLQuery convertToCQLStatement(AttributeList criteriaList, CQLTargetName value){
		if(criteriaList == null || value == null){
			return null;
		}
		Map<String, String> query = new HashMap<String, String>();
		if(value == CQLTargetName.PATIENT){
			query.put(HashmapToCQLQuery.TARGET_NAME_KEY, gov.nih.nci.ncia.domain.Patient.class.getCanonicalName());
		}else if( value == CQLTargetName.STUDY){
			query.put(HashmapToCQLQuery.TARGET_NAME_KEY, gov.nih.nci.ncia.domain.Study.class.getCanonicalName());
		}else if(value == CQLTargetName.SERIES){
			query.put(HashmapToCQLQuery.TARGET_NAME_KEY, gov.nih.nci.ncia.domain.Series.class.getCanonicalName());
		}else if(value == CQLTargetName.IMAGE){
			query.put(HashmapToCQLQuery.TARGET_NAME_KEY, gov.nih.nci.ncia.domain.Image.class.getCanonicalName());
		}
		CQLQuery cqlq = null;		
		DicomDictionary dictionary = AttributeList.getDictionary();
		Iterator<?> iter = dictionary.getTagIterator();		
		while(iter.hasNext()){
			AttributeTag attTag  = (AttributeTag)iter.next();
			String attValue = Attribute.getSingleStringValueOrEmptyString(criteriaList, attTag);
			String nciaAttName = null;
			if(!attValue.isEmpty()){
				nciaAttName = mapDicomTagToNCIATagName(attTag.toString());
			}
			if(nciaAttName != null){
				//wild card is not allowed with grid criteria and should be replaced by empty string 
				if(attValue.equalsIgnoreCase("*")){attValue = "";}
				query.put(nciaAttName, attValue);
			}							
		}								
		try {
			HashmapToCQLQuery h2cql = new HashmapToCQLQuery(new ModelMap(new File("resources/NCIAModelMap.properties")));
			if (query.isEmpty()) {
				query = new HashMap<String, String>();
				query.put(HashmapToCQLQuery.TARGET_NAME_KEY, gov.nih.nci.ncia.domain.Series.class.getCanonicalName());
			}
			cqlq = h2cql.makeCQLQuery(query);
			if(value == CQLTargetName.IMAGE){
				QueryModifier queryModifier = new QueryModifier();
				queryModifier.setCountOnly(true);
				cqlq.setQueryModifier(queryModifier);
			}
			try {
				System.out.println(cqlq.toString());
				System.err.println(ObjectSerializer.toString(cqlq, 
						new QName("http://CQL.caBIG/1/gov.nih.nci.cagrid.CQLQuery", "CQLQuery")));
			} catch (SerializationException e) {
				e.printStackTrace();
			}
			return cqlq;
		} catch (FileNotFoundException e) {
			logger.error(e, e);
			return null;
		} catch (ModelMapException e) {
			logger.error(e, e);			
			return null;
		} catch (IOException e) {
			logger.error(e, e);
			return null;
		} catch (ClassNotFoundException e) {
			logger.error(e, e);
			return null;
		} catch (MalformedQueryException e) {
			logger.error(e, e);
			return null;
		}		
	}
	
	enum CQLTargetName {
		PATIENT,
		STUDY,
		SERIES,
		IMAGE
	}
	
	public Map<String, String> loadNCIAModelMap(InputStream fileInputStream) throws IOException{	
		prop.load(fileInputStream);						
		map = new HashMap<String, String>();		
		Enumeration<Object> enumer = prop.keys();
		while(enumer.hasMoreElements()){
			String name = (String)enumer.nextElement();
			String value = prop.getProperty(name);
			if(!value.isEmpty()){
				map.put(value, name);
			}else{
				return null;
			}	
		}		
		return map;
	}
	
	public String mapDicomTagToNCIATagName(String tag){
		//Mapping is case sensitive therefore if mapping returns null in first attempt
		//system will try to use toUpperCase except "x" and map to NCIA value again
		if(map.get(tag) != null){
			return map.get(tag);
		}else {
			tag = tag.toUpperCase();
			tag = tag.replace("X", "x");
			return map.get(tag);
		}		 
	}
	
	public static void main(String[] args) throws IOException {
		MakeCQL makeCql = new MakeCQL();
		InputStream fis = new FileInputStream("resources/NCIAModelMap.properties");
		makeCql.loadNCIAModelMap(fis);
		
		AttributeList attList = new AttributeList();
		try {
			String[] characterSets = { "ISO_IR 100" };
			SpecificCharacterSet specificCharacterSet = new SpecificCharacterSet(characterSets);			
			{ AttributeTag t = TagFromName.PatientID; Attribute a = new ShortStringAttribute(t,specificCharacterSet); a.addValue("TCGA-08-0514"); attList.put(t,a); }
			{ AttributeTag t = TagFromName.SpecificCharacterSet; Attribute a = new CodeStringAttribute(t); a.addValue(characterSets[0]); attList.put(t,a); }			
		}
		catch (Exception e) {
			e.printStackTrace(System.err);			
		}
		CQLQuery cql = makeCql.convertToCQLStatement(attList, CQLTargetName.SERIES);
		try {
			System.err.println(ObjectSerializer.toString(cql, new QName("http://CQL.caBIG/1/gov.nih.nci.cagrid.CQLQuery", "CQLQuery")));
		} catch (SerializationException e) {			
			e.printStackTrace();
		}	
	}

}
