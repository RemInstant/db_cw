let submitButttom = document.getElementById("submit");

submitButttom.addEventListener('click', () => {
  alert('click');
  let username = document.getElementById('username').value;
  let password = document.getElementById('password').value;
  let data = {
    username,
    password
  };

  let jsonData = JSON.stringify(data);
  fetch('/register', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: jsonData
  })
  .then(response => {
      if (
  })
});