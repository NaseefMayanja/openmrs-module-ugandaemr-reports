package org.openmrs.module.ugandaemrreports.reports;

import org.openmrs.PatientIdentifierType;
import org.openmrs.module.metadatadeploy.MetadataUtils;
import org.openmrs.module.reporting.cohort.definition.CohortDefinition;
import org.openmrs.module.reporting.data.DataDefinition;
import org.openmrs.module.reporting.data.converter.AgeConverter;
import org.openmrs.module.reporting.data.converter.BirthdateConverter;
import org.openmrs.module.reporting.data.converter.DataConverter;
import org.openmrs.module.reporting.data.converter.ObjectFormatter;
import org.openmrs.module.reporting.data.patient.definition.ConvertedPatientDataDefinition;
import org.openmrs.module.reporting.data.patient.definition.PatientIdentifierDataDefinition;
import org.openmrs.module.reporting.data.person.definition.*;
import org.openmrs.module.reporting.dataset.definition.DataSetDefinition;
import org.openmrs.module.reporting.dataset.definition.PatientDataSetDefinition;
import org.openmrs.module.reporting.evaluation.parameter.Mapped;
import org.openmrs.module.reporting.evaluation.parameter.Parameter;
import org.openmrs.module.reporting.report.ReportDesign;
import org.openmrs.module.reporting.report.definition.ReportDefinition;
import org.openmrs.module.ugandaemrreports.data.converter.CalculationResultDataConverter;
import org.openmrs.module.ugandaemrreports.data.converter.ObsDataConverter;
import org.openmrs.module.ugandaemrreports.definition.data.definition.CalculationDataDefinition;
import org.openmrs.module.ugandaemrreports.library.DataFactory;
import org.openmrs.module.ugandaemrreports.library.EIDCohortDefinitionLibrary;
import org.openmrs.module.ugandaemrreports.library.HIVCohortDefinitionLibrary;
import org.openmrs.module.ugandaemrreports.metadata.HIVMetadata;
import org.openmrs.module.ugandaemrreports.reporting.calculation.eid.DateFromBirthDateCalculation;
import org.openmrs.module.ugandaemrreports.reporting.calculation.eid.ExposedInfantMotherCalculation;
import org.openmrs.module.ugandaemrreports.reporting.calculation.eid.ExposedInfantMotherPhoneNumberCalculation;
import org.openmrs.module.ugandaemrreports.reporting.calculation.eid.ExposedInfantSecondDNAPCRDateCalculation;
import org.openmrs.module.ugandaemrreports.reporting.dataset.definition.SharedDataDefintion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * Infants due for Rapid test which is at 18 months of age
 */
@Component
public class SetupInfantDueForRapidTest extends UgandaEMRDataExportManager {
	
	@Autowired
	private DataFactory df;
	
	@Autowired
	SharedDataDefintion sdd;
	
	@Autowired
	EIDCohortDefinitionLibrary eidCohortDefinitionLibrary;
	
	@Autowired
	HIVMetadata hivMetadata;

	@Autowired
	private HIVCohortDefinitionLibrary hivCohortDefinitionLibrary;

	@Override
	public String getExcelDesignUuid() {
		return "0b6ffe40-5526-11e7-b407-15be7a295d59";
	}
	
	@Override
	public ReportDesign buildReportDesign(ReportDefinition reportDefinition) {
		ReportDesign rd = createExcelTemplateDesign(getExcelDesignUuid(), reportDefinition, "EIDDueForRapidTest.xls");
		Properties props = new Properties();
		props.put("repeatingSections", "sheet:1,row:8,dataset:RapidTest");
		props.put("sortWeight", "5000");
		rd.setProperties(props);
		return rd;
	}
	
	@Override
	public String getUuid() {
		return "1ff3e32c-5526-11e7-b407-15be7a295d59";
	}
	
	@Override
	public String getName() {
		return "Infants Due for Rapid Test";
	}
	
	@Override
	public String getDescription() {
		return "Infants List Due for Rapid Test at 18 months of age";
	}
	
	@Override
	public ReportDefinition constructReportDefinition() {
		ReportDefinition rd = new ReportDefinition();
		
		rd.setUuid(getUuid());
		rd.setName(getName());
		rd.setDescription(getDescription());
		rd.addParameters(getParameters());
		rd.addDataSetDefinition("RapidTest", Mapped.mapStraightThrough(constructDataSetDefinition()));
		return rd;
	}
	
	
	@Override
	public String getVersion() {
		return "0.1.5.2";
	}
	
	@Override
	public List<Parameter> getParameters() {
		List<Parameter> l = new ArrayList<Parameter>();
		l.add(df.getStartDateParameter());
		l.add(df.getEndDateParameter());
		return l;
	}
	
	@Override
	public List<ReportDesign> constructReportDesigns(ReportDefinition reportDefinition) {
		List<ReportDesign> l = new ArrayList<ReportDesign>();
		l.add(buildReportDesign(reportDefinition));
		return l;
	}
	
	private DataSetDefinition constructDataSetDefinition() {
		PatientDataSetDefinition dsd = new PatientDataSetDefinition();
		dsd.setName("RapidTest");
		dsd.addParameters(getParameters());
		CohortDefinition enrolledInTheQuarter = hivCohortDefinitionLibrary.getEnrolledInCareBetweenDates();
		CohortDefinition eidDueForRapidTest = df.getPatientsNotIn(eidCohortDefinitionLibrary.getExposedInfantsDueForRapidTest(),enrolledInTheQuarter);
		dsd.addRowFilter(eidDueForRapidTest, "startDate=${startDate},endDate=${endDate}");
		
		//identifier
		// TODO: Standardize this as a external method that takes the UUID of the PatientIdentifier
		PatientIdentifierType exposedInfantNo = MetadataUtils.existing(PatientIdentifierType.class, "2c5b695d-4bf3-452f-8a7c-fe3ee3432ffe");
		DataConverter identifierFormatter = new ObjectFormatter("{identifier}");
		DataDefinition identifierDef = new ConvertedPatientDataDefinition("identifier", new PatientIdentifierDataDefinition(exposedInfantNo.getName(), exposedInfantNo), identifierFormatter);
		
		dsd.addColumn("EID No", identifierDef, (String) null);
		dsd.addColumn("Infant Name", new PreferredNameDataDefinition(), (String) null);
		dsd.addColumn("Birth Date", new BirthdateDataDefinition(), "", new BirthdateConverter("MMM d, yyyy"));
		dsd.addColumn("Age", new AgeDataDefinition(), "", new AgeConverter("{m}"));
		dsd.addColumn("Sex", new GenderDataDefinition(), (String) null);
		dsd.addColumn("Mother Name", new CalculationDataDefinition("Mother Name", new ExposedInfantMotherCalculation()), "", new CalculationResultDataConverter());
		dsd.addColumn("Mother Phone", new CalculationDataDefinition("Mother Phone", new ExposedInfantMotherPhoneNumberCalculation()), "", new CalculationResultDataConverter());
		addColumn(dsd,"Parish",df.getPreferredAddress("address4"));
		addColumn(dsd,"Village",df.getPreferredAddress("address5"));
		dsd.addColumn("Mother ART No", sdd.definition("Mother ART No",  hivMetadata.getExposedInfantMotherARTNumber()), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
		dsd.addColumn("1st PCR Date", sdd.definition("1st PCR Date",  hivMetadata.getFirstPCRTestDate()), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
		dsd.addColumn("1st PCR Results", sdd.definition("1st PCR Results",  hivMetadata.getFirstPCRTestResults()), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
		dsd.addColumn("Breastfeeding Status", sdd.definition("Breastfeeding Status",  hivMetadata.getBreastFeedingStatus()), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
		dsd.addColumn("2nd PCR Date", sdd.definition("2nd PCR Date",  hivMetadata.getSecondPCRTestDate()), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
		dsd.addColumn("2nd PCR Results", sdd.definition("2nd PCR Results",  hivMetadata.getSecondPCRTestResults()), "onOrAfter=${startDate},onOrBefore=${endDate}", new ObsDataConverter());
		dsd.addColumn("Rapid Test Due Date", getRapidTestDueDate(18, "m"), "", new CalculationResultDataConverter());
		
		return dsd;
	}
	
	private DataDefinition getRapidTestDueDate(Integer duration, String durationType) {
		CalculationDataDefinition cdf = new CalculationDataDefinition("Date of Rapid Test",  new DateFromBirthDateCalculation());
		cdf.addCalculationParameter("duration", duration);
		cdf.addCalculationParameter("durationType", durationType);
		return cdf;
	}
	
	private RelationshipsForPersonDataDefinition getMother() {
		RelationshipsForPersonDataDefinition mother = new RelationshipsForPersonDataDefinition();
		
		return mother;
	}
	
	private DataDefinition getFirstDNAPCRDate(Integer duration, String durationType) {
		CalculationDataDefinition cdf = new CalculationDataDefinition("Date of 1st DNA PCR",  new DateFromBirthDateCalculation());
		cdf.addCalculationParameter("duration", duration);
		cdf.addCalculationParameter("durationType", durationType);
		return cdf;
	}
	
	private DataDefinition getSecondDNAPCRDateFromBreastFeedingDate(String q){
		CalculationDataDefinition cd = new CalculationDataDefinition("", new ExposedInfantSecondDNAPCRDateCalculation());
		cd.addParameter(new Parameter("onDate", "On Date", Date.class));
		cd.addCalculationParameter("question", q);
		return cd;
	}
	
}
