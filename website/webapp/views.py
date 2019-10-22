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
		if username and password and passwordc:
			username = username.strip()
			try:
				user=User.objects.get(username=username)
				message="username already exist."
				return render(request,'register.html',{"message":message})
			except:
				if password==passwordc:
					message="Registration successful! You can login now."
					new_user=User.objects.create()
					new_user.username=username
					new_user.password=password
					new_user.save()
					return render(request,'login.html',{"message":message})
				else:
					message="The passwords you input are different, please try agian."
					return render(request,'register.html',{"message":message})

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
					return render(request,'account.html')
				else:
					message="Wrong password."
			except:
				message = "username doesn't exist."
			return render(request,'login.html',{"message":message})
	return render(request, 'login.html')


def account(request):
	return render(request,'account.html')

def attendance(request):
	return render(request,'attendance.html')

def chooseastudent(request):
	return render(request,'chooseastudent.html')