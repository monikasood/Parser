package simplenem12;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SimpleNem12ParserImpl implements SimpleNem12Parser {

	
	@Override
	public Collection<MeterRead> parseSimpleNem12(File simpleNem12File) {
		Map<String,MeterRead> meterReads= new HashMap<>();
		BufferedReader reader =null;
		String line=null;
		try {
			if(simpleNem12File == null)
			{
				throw new Exception("Please provide the file to parse");
			}
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(simpleNem12File), "UTF-8"));
			int rowNum =0;
			String recordType=null;
			MeterRead meterRead = null;
			while(( line = reader.readLine()) !=null)
			{
				String obj[] = line.split(",");
				recordType = obj[0];
				if(rowNum ==0 && !"100".equals(recordType))
				{
					throw new Exception("First record should be of type 100.");
				}
				rowNum++;
				if("200".equals(recordType))
				{
					EnergyUnit eUnit = null;
					try{
						eUnit = EnergyUnit.valueOf(obj[2]);
					}
					catch(IllegalArgumentException le)
					{
						throw new Exception("Energy Unit is invalid"); 
					}
					if(eUnit != null)
					{
						meterRead = meterReads.get(obj[1]);
						if(meterRead == null)
							meterRead = new MeterRead(obj[1], EnergyUnit.valueOf(obj[2]));
						meterReads.put(meterRead.getNmi(), meterRead);
					}
				}
				if("300".equals(recordType))
				{
					if(meterRead ==null)
					{
						throw new Exception("Volume details must be preceeded by NMI details.");
					}
					LocalDate date = LocalDate.parse(obj[1],DateTimeFormatter.BASIC_ISO_DATE);
					if(!isNumeric(obj[2]))
					{
						throw new Exception("Volume value is invalid.");
					}
					BigDecimal volume =new BigDecimal(obj[2]);
					Quality quality=null;
					try {
						quality = Quality.valueOf(obj[3]);
					} 
					catch (IllegalArgumentException e) {
						throw new Exception("Quality value is invalid");
					}
					MeterVolume meterVol = new MeterVolume(volume, quality);
					meterRead.appendVolume(date, meterVol);
					
				}
				
			}
			if(!"900".equals(recordType))
			{
				throw new Exception("Last record should be of type 900.");
				
			}
		}
		catch (Exception e) {
			System.out.println(e.getMessage());
			return Collections.emptyList();
		}
		finally {
			try{
			reader.close();
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
