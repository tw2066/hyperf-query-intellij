<?php

namespace App {
class User extends \Hyperf\Database\Model\Model
{
}
}

(new \App\User())->newQuery()->get('<caret>');
