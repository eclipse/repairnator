"""
This is a script to create a txt-file from every build repairnator has patched
with the following info:
Build URL
Commit URL
All patches along with the name of the tool that generated them

Requirements:
selenium webdriver for python
pymongo
geckodriver (for linux most likely)

To use:
Fill in the constants below with the appropriate info
"""


import pymongo
from pymongo import MongoClient
from selenium import webdriver

"""
Constants used in the mongodb connection
"""
user="" # Username for the database
pwd="" # Password for the above used
db="" # Name of the authentication database (may be left empty)
ip="" # Ip-address of the database
port="" # Port of the database

"""
If one wishes to specify dates, fill these in. By default it will run 
for every script
"""
dateFrom=None # From which date to look for patched builds
dateTo=None # To which date to look for patched builds


"""
Query for each document in inspector and write to a file.
"""
def patches_query(mongoDB, inspectorJson):
    # Fetch the info from patches
    return mongoDB.patches.find({"buildId" : inspectorJson['buildId']})


"""
We will be parsing html, so we need to change the < and > and & icons 
"""
def replace_spec_chars(string):
    string = string.replace("&", "&amp;")
    string = string.replace("<", "&lt;")
    string = string.replace(">", "&gt;")
    string = string.replace("\"", "&quot;")
    string = string.replace("\'", "&apos;")
    return string
    


"""
Query the inspector once and return all documents
"""
def inspector_query(mongoDB):
    # Filter depends on whether we want it to filter dates or not

    global dateFrom
    global dateTo
    
    inspectorFilter = {"status" : "PATCHED"}
    
    if(dateFrom != None and dateTo != None):
        inspectorFilter = {"buildFinishedDate" : { "$gt" : dateFrom, "$lt" : dateTo}, "status" : "PATCHED"}
        
    elif(dateFrom != None and dateTo == None):
        inspectorFilter = {"buildFinishedDate" : { "$gt" : dateFrom}, "status" : "PATCHED"}

    elif(dateFrom == None and dateTo != None):
        inspectorFilter = {"buildFinishedDate" : { "$lt" : dateTo}, "status" : "PATCHED"}

    return mongoDB.inspector.find(inspectorFilter).batch_size(50)

"""
Build the string that will ultimately be written to the txt-file

patchDocs - all docs with the same buildId, so that we gather all patches
diffs - a string of the different diffse
"""
def file_builder(patchDocs, inspectorJson):

    global driver
    buildURL = inspectorJson['travisURL']

    driver.get(buildURL)
    links = driver.find_elements_by_xpath("//a[contains(@href, 'github') and contains(@href, 'commit')]")
    if(len(links) == 1):
        commitURL = links[0].get_attribute("href")
    else:
        return None

    # Where we do have a commit url we build the html file
    f = open(str(inspectorJson['buildId']) + ".html", "w")

    # Write commit and travis url
    f.write("<html>\n<body>\n")
    f.write("<p><a href=\"" + buildURL + "\" id=\"travis-url\">" + buildURL + "</a></p>\n")
    f.write("<p><a href=\"" + commitURL + "\" id=\"commit-url\">" + commitURL + "</a></p>\n")
    
    index = 0
    for json in patchDocs:
        diff = json['diff']
        tool = json ['toolname']
        diff = replace_spec_chars(diff)
        if diff != None and diff != "" and isinstance(diff, str) and tool != None:
            f.write("<pre>" + tool +
                    "<code id=\" " + str(index) + "\" class=\"patch\" title=\"" + tool + "\">\n" 
                    + diff + 
                    "</code></pre>\n")
            index += 1

    f.write("</body>\n</html>\n")
    f.close()
    return 0
        
    
"""
Fetch info and write a file for each build found
"""
def main():
    global db, ip, port, user, pwd

    # Connect by the connection String URI
    client = MongoClient("mongodb://" + user + ":" + pwd + "@" +ip + ":" + port + "/" + db)

    mongoDB = client.repairnator
    
    for inspectorJson in inspector_query(mongoDB):        
        patchesDocs = patches_query(mongoDB, inspectorJson)
        file_builder(patchesDocs, inspectorJson)
        print(inspectorJson['buildId'])
            
            
# Start a webdriver to make sure we can fetch the correct url
driver = webdriver.Firefox()
driver.implicitly_wait(5)
main()
