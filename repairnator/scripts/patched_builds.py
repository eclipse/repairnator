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

    return mongoDB.inspector.find(inspectorFilter)

"""
Build the string that will ultimately be written to the txt-file

patchDocs - all docs with the same buildId, so that we gather all patches
diffs - a string of the different diffse
"""
def txt_builder(patchDocs, inspectorJson):

    global driver
    
    buildURL = inspectorJson['travisURL']

    driver.get(buildURL)
    links = driver.find_elements_by_xpath("//a[contains(@href, 'github') and contains(@href, 'commit')]")
    if(len(links) == 1):
        commitURL = links[0].get_attribute("href")
    else:
        return None
    
    diffs = []
    for json in patchDocs:
        diffs.append(patch_diffs(json))

    diffString = "\n\n\n".join(diffs)

    return "\n".join([buildURL, commitURL, diffString]);
    
    
    

"""
Since we can have multiple patches we will have to handle these one by one as well
Will have to look at tools as well
"""
def patch_diffs(patchJson):
    patchTool = patchJson['toolname']
    patchDiff = patchJson['diff']

    ret = patchTool + " found a patch with the following diff :\n\n" + patchDiff
    print(ret)

    return ret
        
    

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
        txt = txt_builder(patchesDocs, inspectorJson)
        if(txt != None):
            f = open(str(inspectorJson['buildId']) + ".txt", "w")
            f.write(txt)
            f.close();
            
            
# Start a webdriver to make sure we can fetch the correct url
driver = webdriver.Firefox()
driver.implicitly_wait(10)
main()
