package simplenem12;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	
	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
		
		Map<String,MeterRead> meterReads= new HashMap<>();
		
		List<String> errors = new ArrayList<>();
		BiConsumer<CSVRecord,List<String>> validateVolume= (rec,errs)->{
			if(!"300".equals(rec.get(0)))
			{
				errs.add("Record Type should be 300");
			}
			if(!isNumeric(rec.get(2)))
			{
				errs.add("Volume value is invalid." + rec.get(2));
			}
			if(rec.get(3).equals(Quality.A) || rec.get(3).equals(Quality.E))
			{
				errs.add("Quality value is invalid.");
			}
		};
		BiConsumer<CSVRecord, MeterRead> processVolume = (rec,mRead)->{
			if("300".equals(rec.get(0)) && mRead !=null)
			{
				
				validateVolume.accept(rec,errors );
				if(errors.isEmpty())
				{
					LocalDate date = LocalDate.parse(rec.get(1),DateTimeFormatter.BASIC_ISO_DATE);
					BigDecimal volume =new BigDecimal(rec.get(2));
					Quality quality=null;
					quality = Quality.valueOf(rec.get(3));
					
					MeterVolume meterVol = new MeterVolume(volume, quality);
					mRead.appendVolume(date, meterVol);
				}
				else
				{
					System.out.println(errors.size());
					errors.forEach(e->System.out.println(e));
				}
			}
		};
		CSVParser parser =null;
		try 
		{
			parser= CSVFormat.DEFAULT.withDelimiter(',').parse(new InputStreamReader(new FileInputStream(simpleNem12File)));
			String recordType=null;
			MeterRead meterRead=null;
			for(CSVRecord record : parser)
			{
				recordType = record.get(0);
				if("200".equals(recordType))
				{
					EnergyUnit eUnit = null;
					try{
						eUnit = EnergyUnit.valueOf(record.get(2));
					}
					catch(IllegalArgumentException le)
					{
						throw new Exception("Energy Unit is invalid"); 
					}
					if(eUnit != null)
					{
						meterRead = meterReads.get(record.get(2));
						if(meterRead == null)
							meterRead = new MeterRead(record.get(1), EnergyUnit.valueOf(record.get(2)));
						meterReads.put(meterRead.getNmi(), meterRead);
					}
				}
				
				if(recordType.equals("300"))
				{
					
					processVolume.accept(record,meterRead);
				}
				
			}
			if(!"900".equals(recordType))
			{
				throw new Exception("Last record should be of type 900.");
				
			}
		} catch (IOException e2) {
			System.err.println(e2.getMessage());
		}
		catch(Exception le)
		{
			System.err.println(le.getMessage());
		}
		finally {
			try{
			parser.close();
			}
			catch(Exception e1)
			{
				//Ignore this exception
			}
		}
		return meterReads.values();
	}

	private boolean isNumeric(String str)
	{
		  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
		}
}
