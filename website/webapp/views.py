from django.shortcuts import render, HttpResponse, redirect
from django.contrib.auth.forms import UserCreationForm 
from .models import *
from django import forms
# Create your views here.
def index(request):
	return render(request,'index.html')

def register(request):
	if request.method=='POST':
		username= request.POST.get('username',None)
		password= request.POST.get('password',None)
		passwordc =request.POST.get('passwordc',None)
		message = "Please fill in username and passwords."
		if password != passwordc:
			message="The passwords you input are different, please try agian."
			return render(request,'register.html',{"message":message})
		else:
			same_name_user = User.objects.filter(username=username)
			if same_name_user:
				message="username already exist."
				return render(request,'register.html',{"message":message})
			
			message="Registration successful! You can login now."
			new_user=User.objects.create()
			new_user.username=username
			new_user.password=password
			new_user.save()
			message="Registration is successful! You can login now."
			return redirect('/webapp/login')

	return render(request, 'register.html')

def login(request):
	if request.method=='POST':
		username= request.POST.get('username',None)
		password= request.POST.get('password',None)
		message = "Please fill in both username and password."
		if username and password:
			username = username.strip()
			try:
				user=User.objects.get(username=username)
				if user.password == password:
					message="login successful, you can create new Lecture now"
					return redirect('/webapp/account')
				else:
					message="Wrong password."
			except:
				message = "username doesn't exist."
		return render(request,'login.html',{"message":message})
	return render(request, 'login.html')


def account(request):
	if request.method=='POST':
		latitude = request.POST.get('latitude',None)
		longitude = request.POST.get('longitude',None)
		dateTime = request.POST.get('dateTime',None)
		link = request.POST.get('link')
		if latitude and longitude and dateTime:
			message="Lecture added successful, you can see the students' attendance now."
			new_lecture = Lecture()
			new_lecture.latitude=latitude
			new_lecture.longitude=longitude
			new_lecture.dateTime=dateTime
			new_lecture.alink=link
			new_lecture.save()
			return redirect('/webapp/attendance')
												#add argument''', lectureID=new_lecture.lectureID '''
		else: 
			message = "Please fill in latitude, longitude and Date&Time"
			return render(request,'account.html',{"message":message})
	return render(request,'account.html')

def attendance(request):
	if request.method=='GET':
		lectureID = request.GET.get('lectureID')
		# or change to lectureID = lectureID
		attendances = Attendance.objects.filter(lectureID=lectureID)
		return render(request,'attendance.html',{'attendances':attendances})