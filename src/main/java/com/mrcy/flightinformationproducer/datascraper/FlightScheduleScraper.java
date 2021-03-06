/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mrcy.flightinformationproducer.datascraper;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 *
 * @author jwalton
 */
public class FlightScheduleScraper {

    private AirportDataReader airportData = new AirportDataReader();
    private List<AirportInfo> airportsInUse = new ArrayList<>();

    /**
     *
     * @param flightId Airline code and flight# for instance: UAL5842
     * @return
     */
    public List<FlightScheduleData> getFlightSchedules(String flightId) {
        List<FlightScheduleData> flightSchedules = new ArrayList<FlightScheduleData>();

        Document doc = null;
        try {
            doc = Jsoup.connect("http://flightaware.com/live/flight/" + flightId).get();
        } catch (Exception e) {
            System.out.println("Error getting flight scedule data from flightaware.com");
            return flightSchedules;
        }
        Elements scheduleRows = doc.select("table.prettyTable tbody tr");
        String currentDay = getDayString();
        for (Element scheduleRow : scheduleRows) {
            try {
                Elements columnValues = scheduleRow.select("td");
                String date = columnValues.get(0).text();
                if (date.equalsIgnoreCase(currentDay)) {
                    String aircraft = columnValues.get(1).text();
                    String origin = columnValues.get(2).text();
                    String destination = columnValues.get(3).text();
                    String departureString = columnValues.get(4).text().replaceAll("\\u00A0", " ");
                    String arrivalString = columnValues.get(5).text().replaceAll("\\u00A0", " ");
                    arrivalString = currentDay + " " + arrivalString;
                    arrivalString = convertToGmt(arrivalString);
                    departureString = currentDay + " " + departureString;
                    departureString = convertToGmt(departureString);
                    DateFormat scheduleFormat = new SimpleDateFormat("dd-MMM-yyyy hh:mma Z");
                    scheduleFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                    Long departureTime = scheduleFormat.parse(departureString).getTime();
                    Long arrivalTime = scheduleFormat.parse(arrivalString).getTime();

                    FlightScheduleData schedule = new FlightScheduleData(origin, destination, departureTime, arrivalTime);
                    if (destination.length() > 1) {
                        try {
                        String lookup = destination.substring(destination.indexOf("(")+1, destination.indexOf(")"));
                        lookup = lookup.substring(1, lookup.length());
                        AirportInfo airportInfo = airportData.getAirportInfo(lookup);
                        if(airportInfo != null){
                            schedule.setDestLat(airportInfo.getLat());
                            schedule.setDestLon(airportInfo.getLon());
                            airportsInUse.add(airportInfo);
                        } else {
                            System.out.println("Could not find airport: "+lookup);
                        }
                        } catch(Exception e){
                            System.out.println("Error looking up destination lat, lon");
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("Invalid destination: "+destination);
                    }

                    flightSchedules.add(schedule);

                }
            } catch (Exception e) {
                System.out.println("Error parsing out schedule data for " + flightId);
            }
        }
        return flightSchedules;
    }

    public List<AirportInfo> getAirportsInUse(){
        return this.airportsInUse;
    }
    
    private String convertToGmt(String timeString) {
        if (timeString.indexOf("CDT") >= 0) {
            timeString = timeString.replaceAll("CDT", "-0500");
        } else if (timeString.indexOf("CST") >= 0) {
            timeString = timeString.replaceAll("CST", "-0600");
        } else if (timeString.indexOf("EDT") >= 0) {
            timeString = timeString.replaceAll("EDT", "-0400");
        } else if (timeString.indexOf("EST") >= 0) {
            timeString = timeString.replaceAll("EST", "-0500");
        } else if (timeString.indexOf("MDT") >= 0) {
            timeString = timeString.replaceAll("MDT", "-0600");
        } else if (timeString.indexOf("MST") >= 0) {
            timeString = timeString.replaceAll("MST", "-0700");
        } else if (timeString.indexOf("PDT") >= 0) {
            timeString = timeString.replaceAll("PDT", "-0700");
        } else if (timeString.indexOf("PST") >= 0) {
            timeString = timeString.replaceAll("PST", "-0800");
        }

        return timeString;
    }

    private String getDayString() {
        Long currentTime = System.currentTimeMillis();
        DateFormat dateFormat = new SimpleDateFormat("dd-MMM-yyyy");
        Date currentDate = new Date();
        currentDate.setTime(currentTime);
        String currentDay = dateFormat.format(currentDate);
        return currentDay;
    }

    public static void main(String[] args) throws Exception {
        FlightScheduleScraper scraper = new FlightScheduleScraper();

        List<FlightScheduleData> schedule = new ArrayList<FlightScheduleData>();
        schedule = scraper.getFlightSchedules("UAL5842");

        for (int i = 0; i < schedule.size(); i++) {
            System.out.println(schedule.get(i).getJSONObject().toString());
        }

    }
}
