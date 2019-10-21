from django.http import JsonResponse
from .models import *

def getLectures(request):
    lectures = Lecture.objects.all()

    if lectures.exists():
        response = {'status': True, 'lectures': []}
    else:
        response = {'status': False, 'lectures': []}

    for lecture in lectures:
        json = {
            'lectureID': lecture.ID,
            'latitude': lecture.latitude,
            'longitude': lecture.longitude,
            'dateTime': lecture.dateTime
        }

        response['lectures'].append(json)

    return JsonResponse(response)

def checkin(request):
    try:
        userID = request.POST['id']
        lectureID = request.POST['lectureID']

        user = User.objects.get(ID=userID)
        lecture = Lecture.objects.get(ID=lectureID)

        attendance = Attendance()
        attendance.userID = user
        attendance.lectureID = lecture
        attendance.save()

        response = {'status': True}
    except:
        response = {'status': False}

    return JsonResponse(response)

def askhelp(request):
    userID = request.POST['id']
    helpType = request.POST['type']
    response = {'status': True}
    
    user = User.objects.get(ID=userID)

    if helpType == 'ask_picture':
        pictureRequest = PictureRequest()
        pictureRequest.userID = user
        pictureRequest.status = 'available'
        pictureRequest.save()

    elif helpType == 'question':
        questionRequest = QuestionRequest()
        questionRequest.userID = user
        questionRequest.save()

    else:
        response = {'status': False}

    return JsonResponse(response)

def upload(request):
    userID = request.POST['id']
    dataType = request.POST['type']
    IDToBeHelped = request.POST['ID_to_be_helped']
    data = request.POST['data']
    response = {'status': True}

    if dataType == 'picture':
        pictureRequest = PictureRequest.objects.get(userID=IDToBeHelped)
        pictureRequest.data = data
        pictureRequest.status = 'done'
        pictureRequest.save()

    elif dataType == 'voice':
        user = User.objects.get(ID=userID)

        questionRequest = QuestionRequest()
        questionRequest.userID = user
        questionRequest.data = data
        questionRequest.save()

    else:
        response = {'status': False}

    return JsonResponse(response)

def help(request):
    IDToBeHelped = request.POST['ID_to_be_helped']
    pictureRequest = PictureRequest.objects.get(userID=IDToBeHelped)

    if pictureRequest.status == 'available':
        pictureRequest.status = 'taken'
        pictureRequest.save()
        response = {'status': True}
    else:
        response = {'status': False}

    return JsonResponse(response)

numVisted = 0
def check(request):
    userID = request.POST['id']

    if ChosenStudent.objects.filter(userID=userID).exists():
        response = {
            'status': True,
            'type': 'answer_question'
        }

        ChosenStudent.objects.get(userID=userID).delete()

    elif PictureRequest.objects.filter(userID=userID, status='done').exists():
        response = {
            'status': True,
            'type': 'picture_respond',
            'data': PictureRequest.objects.get(userID=userID).data
        }

        PictureRequest.objects.get(userID=userID).delete()

    elif Link.objects.all().exists():
        global numVisted

        numUsers = len(User.objects.all())

        if numVisted < numUsers:
            response = {
                'status': True,
                'type': 'link',
                'data': Link.objects.all()[0].link
            }
            numVisted += 1

            if numVisted == numUsers:
                numVisted = 0
                Link.objects.all().delete()

    else:
        pictureRequests = PictureRequest.objects.filter(status='available').exclude(userID=userID)
        IDs = [pictureRequest.userID.ID for pictureRequest in pictureRequests]

        if len(IDs) > 0:
            response = {
                'status': True,
                'type': 'ask_picture',
                'ID_to_be_helped': IDs
            }

        else:
            response = {'status': False}

    return JsonResponse(response)