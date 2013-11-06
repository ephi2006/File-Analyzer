package edu.georgetown.library.fileAnalyzer.filetest;

import edu.georgetown.library.fileAnalyzer.counter.CheckResult;
import edu.georgetown.library.fileAnalyzer.counter.CounterData;
import edu.georgetown.library.fileAnalyzer.counter.CounterRec;
import edu.georgetown.library.fileAnalyzer.counter.CounterStat;
import edu.georgetown.library.fileAnalyzer.counter.JournalReport1;
import edu.georgetown.library.fileAnalyzer.counter.JournalReport1R4;
import edu.georgetown.library.fileAnalyzer.counter.REV;
import edu.georgetown.library.fileAnalyzer.counter.ReportType;
import gov.nara.nwts.ftapp.FTDriver;
import gov.nara.nwts.ftapp.filetest.DefaultFileTest;
import gov.nara.nwts.ftapp.filter.CSVFilter;
import gov.nara.nwts.ftapp.filter.CounterFilter;
import gov.nara.nwts.ftapp.filter.ExcelFilter;
import gov.nara.nwts.ftapp.filter.TxtFilter;
import gov.nara.nwts.ftapp.ftprop.FTPropEnum;
import gov.nara.nwts.ftapp.importer.DelimitedFileReader;
import gov.nara.nwts.ftapp.stats.Stats;
import gov.nara.nwts.ftapp.stats.StatsGenerator;
import gov.nara.nwts.ftapp.stats.StatsItem;
import gov.nara.nwts.ftapp.stats.StatsItemConfig;
import gov.nara.nwts.ftapp.stats.StatsItemEnum;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

/**
 * Extract all metadata fields from a TIF or JPG using categorized tag defintions.
 * @author TBrady
 *
 */
class CounterValidation extends DefaultFileTest { 
	public static enum Separator{
		DefaultForType(""),
		Comma(","),
		Tab("\t"),
		Semicolon(";"),
		Pipe("|");
		
		public String separator;
		Separator(String s) {separator = s;}
	}
	public static final String DELIM = "Delimiter";
	public static enum FIXABLE {
		NA,YES,NO;
	}
	
	private static enum CounterStatsItems implements StatsItemEnum {
		File(StatsItem.makeStringStatsItem("File", 300)),
		Rec(StatsItem.makeEnumStatsItem(CounterRec.class, "Record").setWidth(60)),
		Stat(StatsItem.makeEnumStatsItem(CounterStat.class, "Compliance").setWidth(170)),
		Fixable(StatsItem.makeEnumStatsItem(FIXABLE.class, "Fixable").setWidth(40)),
		Report(StatsItem.makeStringStatsItem("Counter Report", 150)),
		Version(StatsItem.makeEnumStatsItem(REV.class, "Rev").setWidth(40)),
		ReportTitle(StatsItem.makeStringStatsItem("Report/Cell Title", 150)),
		Message(StatsItem.makeStringStatsItem("Message", 400)),
		CellValue(StatsItem.makeStringStatsItem("Cell Value", 250)),
		Replacement(StatsItem.makeStringStatsItem("Replacement", 250)),
		;
		StatsItem si;
		CounterStatsItems(StatsItem si) {this.si=si;}
		public StatsItem si() {return si;}
	}

	public static enum Generator implements StatsGenerator {
		INSTANCE;
		class CounterStats extends Stats {
			public CounterStats(String key) {
				super(details, key);
				this.setVal(CounterStatsItems.Rec, CounterRec.FILE);
			}
			public CounterStats(File f, String cellname) {
				super(details, getKey(f, cellname));
				this.setVal(CounterStatsItems.Rec, CounterRec.CELL);
			}

		}
		public CounterStats create(String key) {return new CounterStats(key);}
		public CounterStats create(File f, String cellname) {return new CounterStats(f, cellname);}
	}
	public static StatsItemConfig details = StatsItemConfig.create(CounterStatsItems.class);

	long counter = 1000000;
	public CounterValidation(FTDriver dt) {
		super(dt);
		this.ftprops.add(new FTPropEnum(dt, this.getClass().getName(), DELIM, "delim",
				"Delimiter character separating fields - if not default", Separator.values(), Separator.Comma));
		new JournalReport1();
		new JournalReport1R4();
		new ReportType("Database Report 1", REV.R3);
		new ReportType("Database Report 3", REV.R3);
		new ReportType("Book Report 1", REV.R3);
		new ReportType("Book Report 2", REV.R3);
	}
	public String toString() {
		return "Counter Compliance";
	}
	public String getKey(File f) {
		return f.getName();
	}
	
	
	public static String getKey(File f, String cellname) {
		return f.getName() + " [" + cellname + "]";
	}
	
    public String getShortName(){return "Counter";}


    public String getSeparator(File f, String ext) {
		Separator sep = (Separator)this.getProperty(DELIM, Separator.DefaultForType);
    	if (sep != Separator.DefaultForType) return sep.toString();
    	if (ext.equals("CSV")) return ",";
    	if (ext.equals("TXT")) return "\t";
    	return "\t";
    }
    
	public Object fileTest(File f) {
		Stats s = getStats(f);
		String ext = getExt(f);
		String sep = getSeparator(f, ext);
		
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		try {
			if (ext.equals("CSV") || ext.endsWith("TXT")) {
				data = DelimitedFileReader.parseFile(f, sep);
			} else {
				data = readExcel(f);
			}
			CounterData cd = new CounterData(data);
			cd.validate();
			
			if (cd.rpt == null) {
				s.setVal(CounterStatsItems.Stat, CounterStat.UNSUPPORTED_FILE);
				s.setVal(CounterStatsItems.Message, "Report Type could not be identified");
			} else {
				setCellStats(f, cd);
				
				s.setVal(CounterStatsItems.Stat, cd.getStat());
				s.setVal(CounterStatsItems.Report, cd.rpt.name);
				s.setVal(CounterStatsItems.Version, cd.rpt.rev);							
				s.setVal(CounterStatsItems.ReportTitle, cd.title);
				s.setVal(CounterStatsItems.Message, cd.getMessage());				
			}			
		} catch (InvalidFormatException e) {
			s.setVal(CounterStatsItems.Stat, CounterStat.UNSUPPORTED_FILE);
			s.setVal(CounterStatsItems.Message, e.toString());
		} catch (IOException e) {
			s.setVal(CounterStatsItems.Stat, CounterStat.UNSUPPORTED_FILE);
			s.setVal(CounterStatsItems.Message, e.toString());
		} catch (org.apache.poi.hssf.OldExcelFormatException e) {
			s.setVal(CounterStatsItems.Stat, CounterStat.UNSUPPORTED_FILE);
			s.setVal(CounterStatsItems.Message, e.toString());
		} catch (Exception e) {
			e.printStackTrace();
			s.setVal(CounterStatsItems.Stat, CounterStat.UNSUPPORTED_FILE);
			s.setVal(CounterStatsItems.Message, e.toString());
		}
		
		return s.getVal(CounterStatsItems.Stat);
	}
	
	void setCellStats(File f, CounterData cd) {
		for(CheckResult result: cd.results) {
			if (result.stat == CounterStat.VALID) continue;
			Stats stat = Generator.INSTANCE.create(f, result.cell.getCellSort());
			this.dt.types.put(stat.key, stat);
			stat.setVal(CounterStatsItems.Stat, result.stat);
			stat.setVal(CounterStatsItems.Report, cd.rpt.name);
			stat.setVal(CounterStatsItems.Version, cd.rpt.rev);							
			stat.setVal(CounterStatsItems.ReportTitle, result.cell.getCellname());
			stat.setVal(CounterStatsItems.Message, result.message);
			
			stat.setVal(CounterStatsItems.Replacement, result.newVal == null ? "" : result.newVal);
			
			String cellval = "";
			if (result.cell != null) {
				String s = cd.getCellValue(result.cell);
				cellval = (s == null) ? "" : s;
			}
			stat.setVal(CounterStatsItems.CellValue, cellval);
			
			FIXABLE fix = FIXABLE.NA;
			if (result.stat != CounterStat.VALID) {
				fix = result.newVal == null ? FIXABLE.NO : FIXABLE.YES;
			}
			stat.setVal(CounterStatsItems.CellValue, fix);
		}
	}
	
	
	Vector<Vector<String>> readExcel(File f) throws InvalidFormatException, IOException {		
		Vector<Vector<String>> data = new Vector<Vector<String>>();
		Workbook w = WorkbookFactory.create(f);
		if (w.getNumberOfSheets() > 1) throw new InvalidFormatException("Workbook has more than one worksheet");
		if (w.getNumberOfSheets() != 1) throw new InvalidFormatException("Workbook does not have a worksheet");
		Sheet sheet = w.getSheetAt(0);
		for(int r=0; r<sheet.getPhysicalNumberOfRows(); r++) {
			Vector<String> row = new Vector<String>();
			data.add(row);
			Row srow = sheet.getRow(r);
			for(int c=0; c<srow.getLastCellNum(); c++) {
				Cell cell = srow.getCell(c); 
				row.add(cell == null ? null : cell.toString());
			}
		}
		return data;
	}
	
	
	
	
    public Stats createStats(String key){ 
    	return Generator.INSTANCE.create(key);
    }
    public StatsItemConfig getStatsDetails() {
    	return details; 
    }

	public String getDescription() {
		return "Verify Counter Compliance (V3 or V4) of a supported report";
	}
	
	public void initFilters() {
		filters.add(new CounterFilter());
		filters.add(new CSVFilter());
		filters.add(new ExcelFilter());
		filters.add(new TxtFilter());
	}
	
	
	
}