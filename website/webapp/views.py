from django.shortcuts import render, HttpResponse, redirect
from django.contrib.auth.forms import UserCreationForm 
from .models import *
from django import forms
# Create your views here.
def index(request):
	return render(request,'index.html')

def register(request):
	if request.method=='POST':
		form = User(request.POST)
		if form.is_valid():
			username=form.cleaned_data['username']
			password=form.cleaned_data['password']
			registerAdd=User.objects.create_user(username=username,password=password)
			if registerAdd == False:
				return render(request,'register.html')
			else:
				return render(request,'login.html')

	else:
		form = User()

		args = {'form': form}
		return render(request, 'register.html', args)

def login(request):
	if request.method=='POST':
		username= request.POST.get('username')
		password= request.POST.get('password')
		match = auth.authenticate(username=username,password=password)
		if match is not None:
			auth.login(request, match)
			return redirect('/',{'user':match})
		else:
			return render(request,'login.html',{'login_error':'username or password is wrong'})
	return render(request, 'login.html')


def account(request):
	return render(request,'account.html')

def attendance(request):
	return render(request,'attendance.html')