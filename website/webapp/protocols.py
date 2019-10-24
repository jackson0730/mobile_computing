from django.http import JsonResponse, HttpResponse
from .models import *
import random, base64

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
        lectureID = request.POST['lectureID']
        response = {'status': True}
        
        user = User.objects.get(ID=userID)
        lecture = Lecture.objects.get(ID=lectureID)

        if helpType == 'ask_picture':
            pictureRequest = PictureRequest()
            pictureRequest.ID = int(str(userID) + str(lectureID))
            pictureRequest.userID = user
            pictureRequest.status = 'available'
            pictureRequest.lectureID = lecture
            pictureRequest.save(force_insert=True)

        elif helpType == 'question':
            questionRequest = QuestionRequest()
            questionRequest.ID = int(str(userID) + str(lectureID))
            questionRequest.userID = user
            questionRequest.lectureID = lecture
            questionRequest.save(force_insert=True)

        else:
            response = {'status': False}

    except:
        response = {'status': False}

    return JsonResponse(response)

def upload(request):
    try:
        userID = request.POST['id']
        lectureID = request.POST['lectureID']
        dataType = request.POST['type']
        IDToBeHelped = request.POST['ID_to_be_helped']
        data = request.POST['data']

        response = {'status': True}

        if dataType == 'picture':
            ID = int(str(IDToBeHelped) + str(lectureID))
            pictureRequest = PictureRequest.objects.get(ID=ID)
            pictureRequest.data = data
            pictureRequest.status = 'done'
            pictureRequest.save(force_update=True)

        elif dataType == 'voice':
            user = User.objects.get(ID=userID)
            lecture = Lecture.objects.get(ID=lectureID)

            questionRequest = QuestionRequest()
            questionRequest.ID = int(str(userID) + str(lectureID))
            questionRequest.userID = user
            questionRequest.lectureID = lecture
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
        lectureID = request.POST['lectureID']

        ID = int(str(IDToBeHelped) + str(lectureID))
        pictureRequest = PictureRequest.objects.get(ID=ID)

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
        lectureID = request.POST['lectureID']
        ID = int(str(userID) + str(lectureID))

        response = {'status': False}

        if ChosenStudent.objects.filter(userID=userID, lectureID=lectureID).exists():
            response = {
                'status': True,
                'type': 'answer_question'
            }

            ChosenStudent.objects.filter(userID=userID, lectureID=lectureID).delete()

        elif PictureRequest.objects.filter(ID=ID, status='done').exists():
            response = {
                'status': True,
                'type': 'picture_respond',
                'data': PictureRequest.objects.get(ID=ID).data
            }

            PictureRequest.objects.get(ID=ID).delete()

        elif Link.objects.filter(lectureID=lectureID).exists():
            global numVisted
            # Should be Attendance instead of User, will change later
            numUsers = len(Attendance.objects.filter(lectureID=lectureID))

            if numVisted < numUsers:
                response = {
                    'status': True,
                    'type': 'link',
                    'data': Link.objects.get(lectureID=lectureID).alink
                }
                numVisted += 1

                if numVisted == numUsers:
                    numVisted = 0
                    Link.objects.get(lectureID=lectureID).delete()

        else:
            pictureRequests = PictureRequest.objects.filter(status='available', lectureID=lectureID).exclude(ID=ID)
            
            for pictureRequest in pictureRequests:
                ID = int(str(userID) + str(lectureID) + str(pictureRequest.ID))

                if not RjectedPictureRequest.objects.filter(ID=ID).exists():
                    
                    rjectedPictureRequest = RjectedPictureRequest()
                    rjectedPictureRequest.ID = ID
                    rjectedPictureRequest.userID = User.objects.get(ID=userID)
                    rjectedPictureRequest.lectureID = Lecture.objects.get(ID=lectureID)
                    rjectedPictureRequest.requestID = pictureRequest
                    rjectedPictureRequest.save(force_insert=True)

                    response = {
                        'status': True,
                        'type': 'ask_picture',
                        'ID_to_be_helped': pictureRequest.userID.ID
                    }

                    break

    except:
        response = {'status': False}

    return JsonResponse(response)

def webcheck(request):
    lectureID = request.POST['lectureID']
    questions = QuestionRequest.objects.filter(lectureID=lectureID)

    if questions.exists():
        question = questions[0]

        if question.data is None:
            response = {
                'status': True,
                'userID': question.userID.ID,
                'type': 'question'
            }

        else:
            saveRecording(question.data)
            response = {
                'status': True,
                'userID': question.userID.ID,
                'type': 'recording'
            }

        question.delete()

    else:
        response = {'status': False}

    return JsonResponse(response)

def saveRecording(data):
    wav = base64.b64decode(data.encode())
    with open('webapp/static/recording.wav', 'wb') as file:
        file.write(wav)

def pushLink(request):
    try:
        lectureID = request.POST['lectureID']
        lecture = Lecture.objects.get(ID=lectureID)
        link = Link()
        link.lectureID = lecture
        link.alink = lecture.alink
        link.save()
        response = {'status': True}
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
            chosenStudent.lectureID = Lecture.objects.get(ID=lectureID)
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