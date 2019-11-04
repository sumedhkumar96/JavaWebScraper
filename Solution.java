package com.cermati;

import java.io.IOException;
import java.util.ArrayList;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

public class Solution {
    public static void main(String[] args) throws IOException {
        String START_WEB_ADDRESS = "https://www.cermati.com/karir";
        Document doc = RequiredTools.htmlParser(START_WEB_ADDRESS);

        ArrayList<String> departmentList = RequiredTools.departmentListExtractor(doc);

        String jobsTabId;
        Elements jobTab = null;
        for (int tabNumber = 0; tabNumber < departmentList.size(); tabNumber++){
            jobsTabId = "tab" + tabNumber;
            jobTab = RequiredTools.elementsDetectionById(doc, jobsTabId);
            RequiredTools.jobClassifier(jobTab);
        }
        ArrayList<ArrayList<JobClass>> listOfJobObjectsList = RequiredTools.getListOfJobObjectsList();

        //For Debugging Purposes
        /*for (int i = 0; i < listOfJobObjectsList.size(); i++) { 
            for (int j = 0; j < listOfJobObjectsList.get(i).size(); j++) { 
                System.out.print((listOfJobObjectsList.get(i).get(j)).toString() + " ");
            } 
            System.out.println(); 
        }*/

        String outputStatus = RequiredTools.jsonFileCreator(departmentList, listOfJobObjectsList);
        System.out.println(outputStatus);
    }
}

class JobClass{
    String title;
    String location;
    String description;
    String qualification;
    String postedBy;
    @Override
    public String toString() {
        return "Title: "+this.title+" Location: "+this.location+" Description: "+this.description+" Qualification "+this.qualification+" PostedBy: "+this.postedBy;
    }
}

@SuppressWarnings("unchecked") //Used to suppress warning messages at JSON object creation stage
class RequiredTools{
    static ArrayList<ArrayList<JobClass>> listOfJobObjectsList = new ArrayList<ArrayList<JobClass>>();

    static ArrayList<ArrayList<JobClass>> getListOfJobObjectsList(){
        return listOfJobObjectsList;
    }

    static Document htmlParser(String webAddress){
        Document doc = null;
        try {
            doc = Jsoup.connect(webAddress).get();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return doc;
    }

    static ArrayList<String> departmentListExtractor(Document doc){
        String DEPARTMENT_CLASS_NAME = "dept-label text-center";
        Elements departments = RequiredTools.elementsDetectionByClassName(doc, DEPARTMENT_CLASS_NAME);
        ArrayList<String> departmentList = new ArrayList<String>();
        for(Element departmentElement : departments){
            departmentList.add(departmentElement.text());
        }
        return departmentList;
    }

    static Elements elementsDetectionByClassName(Document doc, String className){
        Elements detectedElements = doc.getElementsByClass(className);
        return detectedElements;
    }

    static Elements elementsDetectionById(Document doc, String id){
        Elements detectedElement = doc.getElementById(id).select("a");
        return detectedElement;
    }

    static String locationExtractor(Document doc){
        Elements metaTags = doc.getElementsByTag("meta");
        String locality = null;
        String country = null;
        for (Element metaTag : metaTags) {
            String itemProp = metaTag.attr("itemprop");
            String content = metaTag.attr("content");
            if(itemProp.equals("addressCountry")){
                country = content;
            }
            if(itemProp.equals("addressLocality")){
                locality = content;
            }
        }
        return locality+", "+country;
    }

    static String paragraphExtractor(Document doc, String id){
        String formattedDescription = null;
        Elements jobDescription = null;
        try {
            jobDescription = doc.getElementById(id).select("ul li");
            for(Element descriptionLines : jobDescription) {
                formattedDescription = formattedDescription + descriptionLines.text() + "\n";
        }
        } catch (Exception e) {
            formattedDescription = "No Job posted in this section!";
        }
        return formattedDescription;
    }

    static String postedByExtractor(Document doc, String className){
        String detectedElements = doc.getElementsByClass(className).text();
        return detectedElements;
    }

    static void jobClassifier(Elements jobTab){
        String title;
        String location;
        String description;
        String qualification;
        String postedBy;

        ArrayList<JobClass> listOfJobObjects = new ArrayList<JobClass>();

        for(Element jobElement : jobTab){
            String jobLink = jobElement.attr("href");
            Document jobPage = RequiredTools.htmlParser(jobLink);

            String JOB_TITLE_CLASS_NAME = "job-title";
            Elements jobTitleElements = elementsDetectionByClassName(jobPage, JOB_TITLE_CLASS_NAME);
            title = jobTitleElements.text();

            location = locationExtractor(jobPage);

            String JOB_DESCRIPTION_ID = "st-jobDescription";
            description = paragraphExtractor(jobPage, JOB_DESCRIPTION_ID);

            String QUAIFICATION_DESCRIPTION_ID = "st-qualifications";
            qualification = paragraphExtractor(jobPage, QUAIFICATION_DESCRIPTION_ID);

            String POSTEDBY_CLASS_NAME = "details-title";
            postedBy = postedByExtractor(jobPage, POSTEDBY_CLASS_NAME);

            JobClass jobObject = jobObjectCreator(title, location, description, qualification, postedBy);

            listOfJobObjects.add(jobObject);
        }
        listOfJobObjectsList.add(listOfJobObjects);
    }

    static JobClass jobObjectCreator(String title, String location, String description, String qualification, String postedby){
        JobClass job = new JobClass();
        job.title = title;
        job.location = location;
        job.description =description;
        job.qualification = qualification;
        job.postedBy = postedby;
        return job;
    }

    static String jsonFileCreator(ArrayList<String> departmentList, ArrayList<ArrayList<JobClass>>listOfJobObjectsList) throws IOException {
        JSONObject readyToWriteJsonObject = new JSONObject();
        JSONArray classifiedJobsJsonArray = null;
        JSONObject jobDetailsJsonObject = null;

        for (int jobTypeNumber = 0; jobTypeNumber < listOfJobObjectsList.size(); jobTypeNumber++){
            classifiedJobsJsonArray = new JSONArray();
            for (int jobNumber = 0; jobNumber < listOfJobObjectsList.get(jobTypeNumber).size(); jobNumber++){
                jobDetailsJsonObject = new JSONObject();
                jobDetailsJsonObject.put("title", listOfJobObjectsList.get(jobTypeNumber).get(jobNumber).title);
                jobDetailsJsonObject.put("location", listOfJobObjectsList.get(jobTypeNumber).get(jobNumber).location);
                jobDetailsJsonObject.put("description", listOfJobObjectsList.get(jobTypeNumber).get(jobNumber).description);
                jobDetailsJsonObject.put("qualification", listOfJobObjectsList.get(jobTypeNumber).get(jobNumber).qualification);
                jobDetailsJsonObject.put("postedby", listOfJobObjectsList.get(jobTypeNumber).get(jobNumber).postedBy);
                classifiedJobsJsonArray.add(jobDetailsJsonObject);
            }
            readyToWriteJsonObject.put(departmentList.get(jobTypeNumber), classifiedJobsJsonArray);
        }

        Path currentDirectory = Paths.get(".");
        String outputFilePath = currentDirectory.toString();
        outputFilePath = outputFilePath.substring(0, outputFilePath.length()-1)+"solution.json";
        FileWriter outputFile = null;
        try {
            outputFile = new FileWriter(outputFilePath);
            outputFile.write(readyToWriteJsonObject.toJSONString());
            return "File successfully written!";
        } catch (IOException e) {
            return "File writing failed!";
        } finally {
            outputFile.flush();
            outputFile.close();
        }
    }
}