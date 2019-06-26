import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.Select;


public class Mymain {
	
	//url contains the page to be hit 
	static String url = "http://www.cmegroup.com/trading/equity-index/us-index/e-mini-sandp500_quotes_settlements_options.html";
	static String datesavailable; //A random string variable to hold the dates
	static List<String> availableDateList = new ArrayList<>(); //used to store the list of dates for which the trade data is available on the given link
	static List<String> datearraycopy = new ArrayList<>(); //random list to store temporary date values
	static List<String> userDateRangeList = new ArrayList<>(); //list to store the range of dates entered by users
	static DateFormat df = new SimpleDateFormat("MM/dd/yyyy");  //the date format as per the question
	
	//this is a static method to get the range of dates entered by the user 
	public static List<String> getDaysBetweenDates(Date startdate, Date enddate)
	{
		List<String> dates = new ArrayList<String>(); //List initialized to store the dates 
		Calendar calendar = new GregorianCalendar(); //The  calendar is initialized
		calendar.setTime(startdate); 
		//loop to generate the dates included between the start date and enddate 
		while (calendar.getTime().before(enddate)) 
		{
			Date result = calendar.getTime();
			dates.add(df.format(result));
			calendar.add(Calendar.DATE, 1);
		}
		dates.add(df.format(enddate)); //adding enddate to the the list
		return dates; 
	}
	
	//execution of the program from this method
	public static void main (String args[]) throws InterruptedException, ParseException{
		
		Scanner sc =  new Scanner(System.in); //standard class to get input from the console
		System.out.println("Please enter the start date in Format: Month/Date/Year: for the trade report you want");
		String userbegindate = sc.nextLine(); //takes user's start date 
		Date startDate = df.parse(userbegindate); //parses the string to date format
		System.out.println("Please enter the END date in Format: Month/Date/Year: for the trade report you want");
		String userenddate = sc.nextLine();//takes user's end date
		Date endDate = df.parse(userenddate);//parses the string to date format
		System.out.println("Please enter the destination Path: ");
		String filePath = sc.nextLine(); //takes user's desired destination path
		final File destinationFolder = new File(filePath); 
		//to check if the folder exists from the given filepath
		if(! destinationFolder.exists()){
			if(!destinationFolder.mkdirs()){
				System.out.println("Unable to create folder");
				return;
			}
		}
		
		System.setProperty("webdriver.chrome.driver", "src/chromedriver"); //selenium to initialize the driver for the browser, I used chrome 
		final WebDriver driver = new ChromeDriver(); //trying to get driver for chrome
		driver.get(url); //trying to hit the url given above
		driver.manage().window().maximize(); //open's the window and maximizes the window 
		driver.manage().timeouts().pageLoadTimeout(15, TimeUnit.SECONDS); //initialized 15 sec delay, so that web pages loads properly
		Select select = new Select(driver.findElement(By.id("cmeTradeDate"))); //retrieving the dropdown uing the id of dropdown where user selects the date on the webpage
	    List<WebElement> lists=  select.getOptions();  //retrieving the dropdown options
	    System.out.println("Records available for following dates:"); 
	     
	     //loop to retrieve the list of available dates present on webpage on the above identified dropdown
	     for(WebElement element: lists){
	         datesavailable = element.getAttribute("value");
	         availableDateList.add(datesavailable);
	         System.out.println(datesavailable);
         
	     }
	     
	     //calling the static method(defined above) to get the range of dates entered by the user and adding them all to the userDateRangeList
	     userDateRangeList.addAll(getDaysBetweenDates( startDate, endDate)); 
	     System.out.println("User Date range list are: ");
		 System.out.println(userDateRangeList);		 
		 
		 //for each loop created to get the table data for all the dates for which user wants data
		 for (String datearraycopy : userDateRangeList) {
			
			 //the if statement checks if the date range entered by user is avaliable on the website or not
			if (availableDateList.contains(datearraycopy)) {
				System.out.println("Fetching record for: "+datearraycopy);
				Select dropdown = new Select(driver.findElement(By.id("cmeTradeDate")));  //identifying the dropdown
				dropdown.selectByValue(datearraycopy); //sending the date to the website
				Thread.sleep(45*1000); //waiting to load the page
				try {
						
					final String userDate = datearraycopy;	 //initializing the choosen date and assigning it to the variable
					//creating a new thread to copy the table data and it parses the table and creates the file for the same
					new Thread(new Runnable(){
						public void run(){
							FileWriter fw = null;
							try{
							String fileName= userDate.replace("/", "")+"_OptionsReport.txt"; 
							fw = new FileWriter(new File(destinationFolder,fileName));
							PrintWriter pw = new PrintWriter(fw);
							Document doc= Jsoup.parse(driver.getPageSource()); //parsing the page renedered  through jsoup
							Element table = doc.getElementById("settlementsOptionsProductTable"); //identifying the table
							Elements rows = table.getElementsByTag("tr"); //identifying each record
							
							for (Element row : rows) {
								// System.out.println("row");
								Elements tds = row.getElementsByTag("td"); //identifying each element by tag td
								Elements th = row.getElementsByTag("th");  //identifying each element by tag th
								for (int i = 0; i < tds.size(); i++) {
									String rowdata = tds.get(i).text()+" " +th.text();
									pw.print(rowdata + ","); 
								}
								pw.println();
							}
							pw.println();
							pw.println();
							pw.close(); //closing PrintWriter
							}catch(Exception e){
								e.printStackTrace();
							}
							finally{
							if(fw!=null){
								try {
									fw.close();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
							}
						}
					}).start();
					//Thread.sleep(5000);
				} catch (Exception ioe) {
					ioe.printStackTrace();				
				}
				//Thread.sleep(10000);
				//break;
				System.out.println("Successfully saved at destination.");
			} else {
				System.out.println("The record is not available for "+datearraycopy);
			}

		}
	}
}
