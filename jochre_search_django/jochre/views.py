#coding=utf-8
import json
import requests
import logging

from django.shortcuts import render
from django.shortcuts import redirect
from django.conf import settings
from django.template.defaulttags import register
from django.http import HttpResponse

from jochre.models import KeyboardMapping

def search(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		return redirect('accounts/login/')
	
	username = request.user.username
	ip = get_client_ip(request)
	
	searchUrl = settings.JOCHRE_SEARCH_URL
	advancedSearch = False
	haveResults = False

	query = ''
	if 'query' in request.GET:
		query = request.GET['query']
	
	author = ''
	hasAuthors = False
	if 'author' in request.GET:
		author = "|".join(request.GET.getlist('author'))
		hasAuthors = len(author.replace("|", ""))>0
		
	title = ''
	if 'title' in request.GET:
		title = request.GET['title']
		
	strict = False
	if ('strict') in request.GET:
		strict = True

	authorInclude = True
	if ('authorInclude') in request.GET:
		authorInclude = request.GET['authorInclude']=='true'

	fromYear = ''
	if 'fromYear' in request.GET:
		fromYear = request.GET['fromYear']

	toYear = ''
	if 'toYear' in request.GET:
		toYear = request.GET['toYear']

	sortBy = ''
	if 'sortBy' in request.GET:
		sortBy = request.GET['sortBy']
	
	if hasAuthors or len(title)>0 or len(fromYear)>0 or len(toYear)>0 or strict or sortBy == 'yearAsc' or sortBy == 'yearDesc':
		advancedSearch = True

	displayAdvancedSearch = False
	if advancedSearch:
		displayAdvancedSearch = True
	
	page = 0
	if 'page' in request.GET:
		page = int(request.GET['page'])-1
	
	model = {"query" : query,
			 "authors" : filter(None, author.split("|")),
			 "authorQuery": author,
			 "authorInclude" : authorInclude,
			 "title" : title,
			 "strict" : strict,
			 "fromYear" : fromYear,
			 "toYear" : toYear,
			 "sortBy" : sortBy,
			 "displayAdvancedSearch" : displayAdvancedSearch,
			 "page" : page+1,
			 "JOCHRE_TITLE" : settings.JOCHRE_TITLE,
			 "JOCHRE_CREDITS" : settings.JOCHRE_CREDITS,
			 "JOCHRE_SEARCH_EXT_URL" : settings.JOCHRE_SEARCH_EXT_URL,
			 "RTL" : (not settings.JOCHRE_LEFT_TO_RIGHT),
			 "readOnline" : settings.JOCHRE_READ_ONLINE,
			 "Strings" : settings.JOCHRE_UI_STRINGS,
			 "JOCHRE_CROWD_SOURCE" : settings.JOCHRE_CROWD_SOURCE,
			 "JOCHRE_FONT_LIST" : settings.JOCHRE_FONT_LIST,
			 "JOCHRE_FONT_NAMES" : settings.JOCHRE_FONT_NAMES,
			 "JOCHRE_LANGUAGE_LIST" : settings.JOCHRE_LANGUAGE_LIST,
			 "JOCHRE_LANGUAGE_NAMES" : settings.JOCHRE_LANGUAGE_NAMES,
			 "defaultFontImage" : "images/" + settings.JOCHRE_FONT_LIST[0] + ".png",
			 "ip": ip,
			 }
			 
	if len(query)>0:
		MAX_DOCS=1000
		RESULTS_PER_PAGE=10
		userdata = {"command": "search",
					"query": query,
					"user": username,
					"ip": ip,
					"page": page,
					"resultsPerPage" : RESULTS_PER_PAGE
					}
		if len(author)>0:
			userdata['authors'] = author
		if len(title)>0:
			userdata['title'] = title
		if strict:
			userdata['expand'] = 'false'
		if authorInclude:
			userdata['authorInclude'] = 'true'
		else:
			userdata['authorInclude'] = 'false'
		if len(fromYear)>0:
			userdata['fromYear'] = fromYear
		if len(toYear)>0:
			userdata['toYear'] = toYear
		if len(sortBy)>0:
			if sortBy=='yearAsc':
				userdata['sortBy'] = 'Year'
				userdata['sortAscending'] = 'true'
			elif sortBy=='yearDesc':
				userdata['sortBy'] = 'Year'
				userdata['sortAscending'] = 'false'

		logger.debug("sending request: %s, %s" % (searchUrl, userdata))

		resp = requests.get(searchUrl, userdata)
		
		results = resp.json()
		
		if len(results)==1 and 'parseException' in results[0]:
			model["parseException"] = results[0]["message"]
		else:
			docIds = ''
			totalHits = results['totalHits']
			docs = results['results']

			for result in docs:
				if 'volume' in result:
					if 'titleEnglish' in result:
						result['titleEnglishAndVolume'] = result['titleEnglish'] + ", " + settings.JOCHRE_UI_STRINGS['volume'] + " " + result['volume']
					else:
						result['titleEnglishAndVolume'] = settings.JOCHRE_UI_STRINGS['volume'] + " " + result['volume']
					if 'title' in result:
						result['titleAndVolume'] = result['title'] + ", " + settings.JOCHRE_UI_STRINGS['volumeRTL']  + " " +  result['volume']
					else:
						result['titleAndVolume'] = settings.JOCHRE_UI_STRINGS['volumeRTL']  + " " +  result['volume']
				else:
					if 'titleEnglish' in result:
						result['titleEnglishAndVolume'] = result['titleEnglish']
					else:
						result['titleEnglishAndVolume'] = ''
					if 'title' in result:
						result['titleAndVolume'] = result['title']
					else:
						result['titleAndVolume'] = ''
				
				if len(docIds)>0:
					docIds += ','
				docIds += str(result['docId'])
			
			if len(docs)>0:
				haveResults = True
				userdata = {"command": "snippets",
							"snippetCount": 8,
							"query": query,
							"docIds": docIds,
							"user": username,
							"ip": ip,
							}
				if strict:
					userdata['expand'] = 'false'
				resp = requests.get(searchUrl, userdata)
				model["results"] = docs
				start = page * RESULTS_PER_PAGE + 1
				end = start + RESULTS_PER_PAGE - 1
				if end > totalHits: end = totalHits
				model["start"] = start
				model["end"] = end
				model["resultCount"] = totalHits

				pageLinks = []
				lastPage = totalHits // RESULTS_PER_PAGE
				startPage = page - 3
				if startPage < 0: startPage = 0
				endPage = startPage + 6
				if endPage > lastPage: endPage = lastPage
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["first"], "val":1, "active":True})
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["prev"], "val":page, "active": (page > 0)})
				if startPage>0: pageLinks.append({"name":"..", "val":0, "active": False})
				for i in range(startPage, endPage+1):
					pageLinks.append({"name":"%d" % (i+1), "val": i+1, "active": i != page })
				if endPage<lastPage: pageLinks.append({"name":"..", "val":0, "active": False})
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["next"], "val":page+2, "active": (page < lastPage)})
				pageLinks.append({"name":settings.JOCHRE_UI_STRINGS["last"], "val":lastPage+1, "active":True})
				model["pageLinks"] = pageLinks

				foundResults = settings.JOCHRE_UI_STRINGS['foundResults'] % (totalHits, start, end)
				model["foundResults"] = foundResults
				
				if 'foundResultsRTL' in settings.JOCHRE_UI_STRINGS:
					foundResultsRTL = settings.JOCHRE_UI_STRINGS['foundResultsRTL'] % (totalHits, start, end)
					model["foundResultsRTL"] = foundResultsRTL
				
				snippetMap = resp.json()
	
				for result in docs:
					bookId = result['name']
					docId = result['docId']
					snippetObj = snippetMap[str(docId)]
					
					if 'snippetError' in snippetObj:
						snippetError = snippetObj['snippetError']
						result['snippetError'] = snippetError
						
					else:
						snippets = snippetObj['snippets']
						snippetsToSend = []
						for snippet in snippets:
							snippetJson = json.dumps(snippet)
							snippetText = snippet.pop("text", "")
							
							userdata = {"command": "imageSnippet",
										"snippet": snippetJson,
										"user": username}

							logger.debug("sending request: %s, %s" % (settings.JOCHRE_SEARCH_EXT_URL, userdata))

							req = requests.Request(method='GET', url=settings.JOCHRE_SEARCH_EXT_URL, params=userdata)
							preparedReq = req.prepare()
							snippetImageUrl = preparedReq.url
							pageNumber = snippet['pageIndex']
							
							snippetDict = {"snippetText" : snippetText,
										   "pageNumber": pageNumber,
										   "imageUrl": snippetImageUrl }
							
							if settings.JOCHRE_READ_ONLINE:
								urlPageNumber = pageNumber / 2 * 2;
								snippetReadUrl = u"https://archive.org/stream/" + bookId + u"#page/n" + str(urlPageNumber) + u"/mode/2up";
								snippetDict["readOnlineUrl"] = snippetReadUrl

							snippetsToSend.append(snippetDict)
							result['snippets'] = snippetsToSend
					
	model["haveResults"] = haveResults
	return render(request, 'search.html', model)

@register.filter
def get_item(dictionary, key):
	return dictionary[key]

@register.filter
def addstr(arg1, arg2):
	"""concatenate arg1 & arg2"""
	return str(arg1) + str(arg2)

def get_client_ip(request):
	x_forwarded_for = request.META.get('HTTP_X_FORWARDED_FOR')
	if x_forwarded_for:
		ip = x_forwarded_for.split(',')[0]
	else:
		ip = request.META.get('REMOTE_ADDR')
	return ip

def keyboard(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		raise ValueError('User not logged in.')
	
	username = request.user.username
	mappings = KeyboardMapping.objects.filter(user=request.user)

	if len(mappings)==1:
		mapping = json.loads(mappings[0].mapping)
		enabled = mappings[0].enabled
	else:
		mapping = settings.KEYBOARD_MAPPINGS
		enabled = settings.KEYBOARD_MAPPINGS_ENABLED

	model = {"mapping" : mapping, "enabled" : enabled}

	return HttpResponse(json.dumps(model), content_type='application/json')

def updateKeyboard(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		raise ValueError('User not logged in.')

	if request.method == 'POST':
		if 'action' in request.POST and request.POST['action']=='default':
			KeyboardMapping.objects.filter(user=request.user).delete()
			logging.debug('deleted mapping')
		else:
			newVals = {}
			if 'from' in request.POST and 'to' in request.POST:
				fromKeys = request.POST.getlist('from')
				logger.info("fromKeys: %s" % fromKeys)
				toKeys = request.POST.getlist('to')
				logger.info("toKeys: %s" % toKeys)
				newMapping = dict(zip(fromKeys, toKeys))
				newMapping = {k: v for k, v in newMapping.items() if v != "" and k != ""}
				newMappingJson = json.dumps(newMapping)

				logging.debug('newMapping: %s' % newMappingJson)
				newVals['mapping'] = newMappingJson
			if 'enabled' in request.POST:
				newVals['enabled'] = True
			else:
				newVals['enabled'] = False

			obj, created = KeyboardMapping.objects.update_or_create(
				user=request.user,
				defaults=newVals,
			)

	model = {"result": "success"}
	return HttpResponse(json.dumps(model), content_type='application/json')

def contents(request):
	logger = logging.getLogger(__name__)

	if not request.user.is_authenticated:
		return redirect('accounts/login/')

	searchUrl = settings.JOCHRE_SEARCH_URL
	username = request.user.username
	ip = get_client_ip(request)

	contents = ''
	if 'doc' in request.GET:
		doc = request.GET['doc']

		userdata = {"command": "contents",
					"docName": doc,
					"user": username,
					"ip": ip,
					}

		logger.debug("sending request: %s, %s" % (searchUrl, userdata))

		resp = requests.get(searchUrl, userdata)
		contents = resp.text

	model = {"contents": contents,
					"RTL" : (not settings.JOCHRE_LEFT_TO_RIGHT)
					}

	return render(request, 'contents.html', model)