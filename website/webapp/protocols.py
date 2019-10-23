from django.http import JsonResponse, HttpResponse
from .models import *
import random

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
        attendance.ID = int(str(userID) + str(lectureID))
        attendance.userID = user
        attendance.lectureID = lecture
        attendance.save(force_insert=True)

        response = {'status': True}
    except:
        response = {'status': False}

    return JsonResponse(response)

def askhelp(request):
    try:
        userID = request.POST['id']
        helpType = request.POST['type']
        response = {'status': True}
        
        user = User.objects.get(ID=userID)

        if helpType == 'ask_picture':
            pictureRequest = PictureRequest()
            pictureRequest.userID = user
            pictureRequest.status = 'available'
            pictureRequest.save(force_insert=True)

        elif helpType == 'question':
            questionRequest = QuestionRequest()
            questionRequest.userID = user
            questionRequest.save(force_insert=True)

        else:
            response = {'status': False}

    except:
        response = {'status': False}

    return JsonResponse(response)

def upload(request):
    try:
        userID = request.POST['id']
        dataType = request.POST['type']
        IDToBeHelped = request.POST['ID_to_be_helped']
        data = request.POST['data']
        response = {'status': True}

        if dataType == 'picture':
            pictureRequest = PictureRequest.objects.get(userID=IDToBeHelped)
            pictureRequest.data = data
            pictureRequest.status = 'done'
            pictureRequest.save(force_update=True)

        elif dataType == 'voice':
            user = User.objects.get(ID=userID)

            questionRequest = QuestionRequest()
            questionRequest.userID = user
            questionRequest.data = data
            questionRequest.save(force_insert=True)

        else:
            response = {'status': False}

    except:
        response = {'status': False}

    return JsonResponse(response)

def help(request):
    try:
        IDToBeHelped = request.POST['ID_to_be_helped']
        pictureRequest = PictureRequest.objects.get(userID=IDToBeHelped)

        if pictureRequest.status == 'available':
            pictureRequest.status = 'taken'
            pictureRequest.save(force_update=True)
            response = {'status': True}
        else:
            response = {'status': False}

    except:
        response = {'status': False}

    return JsonResponse(response)

numVisted = 0
def check(request):
    try:
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
            # Should be Attendance instead of User, will change later
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
    except:
        response = {'status': False}

    return JsonResponse(response)

def selectAStudent(request):
    try:
        lectureID = request.POST['lectureID']
        students = Attendance.objects.filter(lectureID=lectureID)

        if students.exists():
            student = random.choice(students)
            chosenStudent = ChosenStudent()
            chosenStudent.userID = student.userID
            chosenStudent.save(force_insert=True)

            response = {'status': True}

        else:
            response = {'status': False}

    except:
        response = {'status': False}

    return JsonResponse(response)

def register(request):
    try:
        username = request.POST['username']
        password = request.POST['password']

        user = User.objects.filter(username=username, password=password)

        if user.exists():
            response = {'status': False}
        else:
            user = User()
            user.username = username
            user.password = password
            user.save(force_insert=True)

            response = {'status': True}
    except:
        response = {'status': False}

    return JsonResponse(response)

def login(request):
    try:
        username = request.POST['username']
        password = request.POST['password']
        user = User.objects.get(username=username, password=password)

        response = {'status': True, 'ID': user.ID}
    except:
        response = {'status': False}

    return JsonResponse(response)